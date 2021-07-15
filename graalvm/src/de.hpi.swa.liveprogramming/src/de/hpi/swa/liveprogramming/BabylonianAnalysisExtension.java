/*
 * Copyright (c) 2020, Software Architecture Group, Hasso Plattner Institute.
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.liveprogramming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.hpi.swa.liveprogramming.types.*;
import org.graalvm.tools.api.lsp.LSPCommand;
import org.graalvm.tools.api.lsp.LSPExtension;
import org.graalvm.tools.api.lsp.LSPServerAccessor;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.liveprogramming.types.AbstractProbe.AssertionProbe;
import de.hpi.swa.liveprogramming.types.AbstractProbe.ExampleProbe;
import de.hpi.swa.liveprogramming.types.AbstractProbe.OrphanProbe;
import de.hpi.swa.liveprogramming.types.AbstractProbe.StatementProbe;
import de.hpi.swa.liveprogramming.types.AbstractProbe.StatementProbeWithExpression;
import de.hpi.swa.liveprogramming.types.BabylonianAnalysisResult.BabylonianAnalysisFileResult;
import de.hpi.swa.liveprogramming.types.BabylonianAnalysisResult.BabylonianAnalysisTerminationResult;

import com.oracle.truffle.tools.utils.json.JSONArray;

import static de.hpi.swa.liveprogramming.types.AbstractProbe.*;

@Registration(id = BabylonianAnalysisExtension.ID, name = BabylonianAnalysisExtension.NAME, version = BabylonianAnalysisExtension.VERSION, services = LSPExtension.class)
public class BabylonianAnalysisExtension extends TruffleInstrument implements LSPExtension {
	protected static final String ID = "babylonian-analysis-lsp-extension";
	protected static final String NAME = "Babylonian Analysis LSP Extension";
	protected static final String VERSION = "0.1";

	public List<LSPCommand> getCommands() {
		return Arrays.asList(new BabylonianAnalysisCommand());
	}

	public static class BabylonianAnalysisCommand implements LSPCommand {

		public static final String EXAMPLE_PREFIX = "<Example ";
		private static final String PROBE_PREFIX = "<Probe ";
		private static final String ASSERTION_PREFIX = "<Assertion ";
		private static final String BABYLONIAN_ANALYSIS_RESULT_METHOD = "textDocument/babylonianAnalysisResult";
		private static final Pattern EXTRACT_IDENTIFIER_AND_PARAMETERS = Pattern.compile("([a-zA-Z0-9]*)\\(([a-zA-Z0-9\\-_, ]*)\\)");
		private static final DocumentBuilder XML_PARSER;
		private static final String ASYNC_WORKER_NAME = "LS Babylonian Async Updater";
		private static final InteropLibrary INTEROP = InteropLibrary.getUncached();

		private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(0, r -> new Thread(r, ASYNC_WORKER_NAME));
		private long startMillis;

		static {
			try {
				XML_PARSER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
		}

		public String getName() {
			return "babylonian_analysis";
		}

		public Object execute(LSPServerAccessor server, Env envInternal, List<Object> arguments) {
			startMillis = System.currentTimeMillis();
			URI targetURI = URI.create((String) arguments.get(0));
			List<FilePos> probes = JSON.map((JSONArray) arguments.get(1), FilePos::fromJSON);
			List<ExampleActive> exampleActives = JSON.map((JSONArray) arguments.get(2), ExampleActive::fromJSON);

			Set<URI> openFileURIs = server.getOpenFileURI2LangId().keySet();

			BabylonianAnalysisResult result = new BabylonianAnalysisResult();

			for (URI uri : openFileURIs) {
				Source source = server.getSource(uri);
				if (source != null && source.hasCharacters()) {
					BabylonianAnalysisFileResult fileResult = result.getOrCreateFile(uri, source.getLanguage());
					addRangeSelectionProbes(fileResult, probes, source);
					scanDocument(fileResult, source, exampleActives);
                    if (source.getCharacters().toString().contains(EXAMPLE_PREFIX)) {
						try {
							envInternal.parse(source).call();
						} catch (Throwable e) {
							return BabylonianAnalysisTerminationResult.create(startMillis, e.getMessage());
						}
                    }
				}
			}

			ScheduledFuture<?> future = sendDecorationsPeriodically(server, result);
			try {
				for (BabylonianAnalysisFileResult file : result.getFileResults()) {
					for (ExampleProbe example : file.getExamples()) {
						runExampleInstrumented(envInternal, result, example);
					}
				}
			} finally {
				future.cancel(true);
			}
			return BabylonianAnalysisTerminationResult.create(startMillis, result);
		}

		public int getTimeoutMillis() {
			return 10000;
		}

		public Object onTimeout(List<Object> arguments) {
			return BabylonianAnalysisTerminationResult.create(startMillis, "Babylonian analysis timed out.");
		}

		private static void addRangeSelectionProbes(BabylonianAnalysisFileResult fileResult, List<FilePos> probeRequests, Source source) {
			for (FilePos request : probeRequests) {
				String line = source.getCharacters(request.getLine()).toString();
				String expression = line.substring(request.getStart(), request.getEnd());
				fileResult.addProbe(request, new SelectionProbe(null, expression, request));
			}
		}

		private static void scanDocument(BabylonianAnalysisFileResult fileResult, Source source, List<ExampleActive> exampleActives) {
			for (int lineNumber = 1; lineNumber <= source.getLineCount(); lineNumber++) {
				String line = source.getCharacters(lineNumber).toString();
				if (line.contains(EXAMPLE_PREFIX)) {
					LinkedHashMap<String, String> attributes = getAttributesOrNull(line, EXAMPLE_PREFIX);
					if (attributes == null) {
						break; // Skip line, insufficient attributes for example
					}
					FunctionDefinition functionDefinition = findFunctionDefinition(lineNumber, source);
					if (functionDefinition == null) {
						break; // End of source reached
					}
					if (attributes.keySet().containsAll(functionDefinition.parameters)) {
						final int currentLine = lineNumber;
						ExampleActive activeRef = exampleActives.stream().filter(i -> i.getLineNumber() == currentLine).findFirst().orElse(null);
						if (activeRef != null && activeRef.isActive()) {
							fileResult.addExample(new ExampleProbe(line, new FilePos(currentLine), source.getLanguage(), functionDefinition, attributes));
						}
					}
				}
			}
		}

		private static FunctionDefinition findFunctionDefinition(int startLineNumber, Source source) {
			for (int lineNumber = startLineNumber; lineNumber < source.getLineCount(); lineNumber++) {
				String line = source.getCharacters(lineNumber).toString();
				if (line.contains(EXAMPLE_PREFIX)) {
					continue; // Skip consecutive examples
				}
				Matcher m = EXTRACT_IDENTIFIER_AND_PARAMETERS.matcher(line);
				if (m.find()) {
					// FIXME: workaround for R functions
					String identifier;
					if (line.contains(" <- function")) {
						identifier = line.substring(0, line.indexOf(" <- function"));
					} else {
						identifier = m.group(1);
					}
					return new FunctionDefinition(identifier, m.group(2).replaceAll(" ", "").split(","));
				} else {
					printError("Unable to find function definition in:\n" + line);
					return null;
				}
			}
			return null;
		}

		private static int findTriggerLine(int startLineNumber, Source source) {
			for (int lineNumber = startLineNumber; lineNumber < source.getLineCount(); lineNumber++) {
				String line = source.getCharacters(lineNumber).toString();
				if (line.isBlank() || line.contains(PROBE_PREFIX) || line.contains(ASSERTION_PREFIX)) {
					continue; // Skip consecutive probes and assertions
				}
				if (!line.isBlank()) {
					return lineNumber; // Trigger line must not be blank
				}
			}
			return -1;
		}

		private static LinkedHashMap<String, String> getAttributesOrNull(String line, String tagPrefix) {
			int startIndex = line.indexOf(tagPrefix);
			int endIndex = line.lastIndexOf('>') + 1;
			assert startIndex > 0;
			if (!(endIndex > 0)) {
				return null;
			}
			String xmlSource = line.substring(startIndex, endIndex);
			try {
				Element xml = XML_PARSER.parse(new ByteArrayInputStream(xmlSource.getBytes())).getDocumentElement();
				LinkedHashMap<String, String> parametersAndValues = new LinkedHashMap<>();
				NamedNodeMap attributes = xml.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					Node item = attributes.item(i);
					parametersAndValues.put(item.getNodeName(), item.getNodeValue());
				}
				return parametersAndValues;
			} catch (SAXException | IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		public static final class FunctionDefinition {
			private final String identifier;
			private final LinkedHashSet<String> parameters;

			private FunctionDefinition(String identifier, String[] parameters) {
				this.identifier = identifier;
				this.parameters = new LinkedHashSet<>();
				for (String parameter : parameters) {
					this.parameters.add(parameter);
				}
			}

			public String getIdentifier() {
				return identifier;
			}

			public LinkedHashSet<String> getParameters() {
				return parameters;
			}
		}

		private ScheduledFuture<?> sendDecorationsPeriodically(LSPServerAccessor server, BabylonianAnalysisResult result) {
			return scheduler.scheduleAtFixedRate(new Runnable() {
				public void run() {
					server.sendCustomNotification(BABYLONIAN_ANALYSIS_RESULT_METHOD, result.toJSON());
				}
			}, 250, 500, TimeUnit.MILLISECONDS);
		}

		private static void runExampleInstrumented(Env env, BabylonianAnalysisResult result, ExampleProbe example) {
			String languageId = example.getLanguageId();
			LanguageInfo languageInfo = env.getLanguages().get(languageId);
			Object scope = env.getScope(languageInfo);
			String targetIdentifier = example.getTargetIdentifier();
			Object targetObject;
			if (!INTEROP.isMemberReadable(scope, targetIdentifier)) {
				String error = targetIdentifier + " not readable";
				example.addObservedValue(ObjectInformation.createError("<unknown>", error, error));
				return;
			}
			try {
				targetObject = INTEROP.readMember(scope, targetIdentifier);
			} catch (UnsupportedMessageException | UnknownIdentifierException e) {
				example.addObservedValue(ObjectInformation.createError("<unknown>", e.getMessage(), e.getMessage()));
				return;
			}
			if (!INTEROP.isExecutable(targetObject)) {
				String error = targetIdentifier + " not executable";
				example.addObservedValue(ObjectInformation.createError("<unknown>", error, error));
				return;
			}
			final Object[] arguments;
			try {
				arguments = getExampleArguments(env, languageId, example.getTargetArgumentExpressions());
			} catch (Throwable e) {
				example.addObservedValue(ObjectInformation.createError("<unknown>", e.getMessage(), e.getMessage()));
				return;
			}
			BabylonianEventNodeFactory babylonianEventNodeFactory = new BabylonianEventNodeFactory(env, result, example);
			ArrayList<EventBinding<ExecutionEventNodeFactory>> bindings = new ArrayList<>();
			for (SourceSectionFilter filter : result.getSourceSectionFilters()) {
				bindings.add(env.getInstrumenter().attachExecutionEventFactory(filter, babylonianEventNodeFactory));
			}
			try {
				Object exampleResult = INTEROP.execute(targetObject, arguments);
				ObjectInformation info = ObjectInformation.create(example.getInvocationExpression(), exampleResult);
				example.addObservedValue(info);
			} catch (Throwable e) {
				example.addObservedValue(ObjectInformation.createError("<unknown>", e.getMessage(), e.getMessage()));
			} finally {
				for (EventBinding<ExecutionEventNodeFactory> binding : bindings) {
					binding.dispose();
				}
			}
		}

		private static Object[] getExampleArguments(Env env, String languageId, String[] expressions) throws Throwable {
			final Object[] arguments = new Object[expressions.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = env.parse(Source.newBuilder(languageId, expressions[i], "<argument expression>").build()).call();
			}
			return arguments;
		}

		private static final class BabylonianEventNodeFactory implements ExecutionEventNodeFactory {
			private final Env env;
			private final BabylonianAnalysisResult result;
			private final ExampleProbe example;

			private BabylonianEventNodeFactory(Env env, BabylonianAnalysisResult result, ExampleProbe example) {
				this.env = env;
				this.result = result;
				this.example = example;
			}

			public ExecutionEventNode create(EventContext context) {
				return new BabylonianEventNode(env, result, example, context);
			}

			private static final class BabylonianEventNode extends ExecutionEventNode {
				private static final String INLINE_PROBE_EXPRESSION_NAME = "<probe>";

				@Child
				private ExecutableNode inlineExecutionNode;

				private final Env env;
				private final BabylonianAnalysisResult result;
				private final ExampleProbe example;
				private final EventContext context;

				private BabylonianEventNode(Env env, BabylonianAnalysisResult result, ExampleProbe example, EventContext context) {
					this.env = env;
					this.result = result;
					this.example = example;
					this.context = context;
				}

				@Override
				public void onReturnValue(VirtualFrame frame, Object value) {
					SourceSection section = context.getInstrumentedSourceSection();
					Source source = section.getSource();
					Function<String, Object> inlineEvalutator = expression -> {
						try {
							return executeInline(frame, Source.newBuilder(source.getLanguage(), expression, INLINE_PROBE_EXPRESSION_NAME).build());
						} catch (Exception e) {
							return e.getMessage();
						}
					};
					BabylonianAnalysisFileResult fileResult = result.getOrCreateFile(toVSCodeURI(source.getURI()), source.getLanguage());
					FilePos pos = FilePos.fromSourceSection(section);
					AbstractProbe probe = fileResult.get(pos);
					if (probe == null) {
						probe = new OrphanProbe(null, pos);
						fileResult.addProbe(pos, probe);
					}
					probe.apply(example, section, value, inlineEvalutator);
				}

				@Override
				protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
					onReturnValue(frame, exception.getMessage());
				}

				private Object executeInline(VirtualFrame frame, Source source) {
					CompilerDirectives.transferToInterpreterAndInvalidate();
					ExecutableNode newNode = env.parseInline(source, context.getInstrumentedNode(), frame.materialize());
					if (inlineExecutionNode == null) {
						inlineExecutionNode = insert(newNode);
					} else {
						inlineExecutionNode.replace(newNode);
					}
					notifyInserted(inlineExecutionNode);
					return inlineExecutionNode.execute(frame);
				}
			}
		}
	}

	/**
	 * Convert Windows URIs (e.g. "file:///c%3A/Users/Bob/test.js") to Truffle Source URIs (e.g.
	 * "file:///C:/Users/Bob/test.js").
	 */
	public static URI toSourceURI(URI uri) {
		String string = uri.toString();
		if (string.indexOf("%3A") == 9) {
			return URI.create(string.substring(0, 8) + string.substring(8, 9).toUpperCase() + ":/" + string.substring(13));
		} else {
			return uri;
		}
	}

	/**
	 * Reverse function of {@link #toSourceURI(URI)}.
	 */
	private static URI toVSCodeURI(URI uri) {
		String string = uri.toString();
		if (string.lastIndexOf(':') == 9) {
			return URI.create(string.substring(0, 8) + string.substring(8, 9).toLowerCase() + "%3A/" + string.substring(11));
		} else {
			return uri;
		}
	}

	private static void printError(String message) {
		// Checkstyle: stop
		System.err.println(message);
		// Checkstyle: resume
	}

	@Override
	protected void onCreate(Env env) {
		env.registerService(this);
	}
}
