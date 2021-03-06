/*
 * Copyright (c) 2020, Software Architecture Group, Hasso Plattner Institute.
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.liveprogramming.types;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.utils.json.JSONArray;
import com.oracle.truffle.tools.utils.json.JSONException;
import com.oracle.truffle.tools.utils.json.JSONObject;

import de.hpi.swa.liveprogramming.BabylonianAnalysisExtension.BabylonianAnalysisCommand;
import de.hpi.swa.liveprogramming.BabylonianAnalysisExtension.BabylonianAnalysisCommand.FunctionDefinition;
import de.hpi.swa.liveprogramming.types.BabylonianAnalysisResult.ProbeType;

public abstract class AbstractProbe {
    private static final InteropLibrary LIB = InteropLibrary.getFactory().getUncached();

    private final String exampleNameOrNull;
    private final FilePos pos;
    private final HashMap<String, ArrayList<ObjectInformation>> example2ObservedValues = new HashMap<>();

    public AbstractProbe(String exampleNameOrNull, FilePos pos) {
        this.exampleNameOrNull = exampleNameOrNull;
        this.pos = pos;
    }

    public final void apply(ExampleProbe example, SourceSection section, Object result, Function<String, Object> inlineEvaluator) {
        if (exampleNameOrNull == null || exampleNameOrNull.equals(example.getExampleName())) {
            example2ObservedValues.computeIfAbsent(example.getExampleName(), n -> new ArrayList<>()).add(getObjectInformation(example, section, result, inlineEvaluator));
        }
    }

    public final void addObservedValue(ObjectInformation value) {
        example2ObservedValues.computeIfAbsent(getExampleName(), n -> new ArrayList<>()).add(value);
    }

    public final String getExampleName() {
        return exampleNameOrNull;
    }

    public FilePos getPos() {
        return pos;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("probeType", getProbeType());
        json.put("pos", pos.toJSON());
        JSONArray examples = new JSONArray();
        for (Entry<String, ArrayList<ObjectInformation>> entry : example2ObservedValues.entrySet()) {
            JSONObject example = new JSONObject();
            example.put("exampleName", entry.getKey());
            JSONArray observedValuesJSON = new JSONArray();
            for (ObjectInformation value : entry.getValue()) {
                observedValuesJSON.put(value.getJSON());
            }
            example.put("observedValues", observedValuesJSON);
            examples.put(example);
        }
        json.put("examples", examples);
        return json;
    }

    protected abstract ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator);

    protected abstract ProbeType getProbeType();

    public static final class AssertionProbe extends AbstractProbe {
        public static final String ASSERTION_EXPECTED_ATTRIBUTE = ":expected";
        public static final String ASSERTION_EXPRESSION_ATTRIBUTE = ":expression";

        private final String expression;
        private final boolean isExpectedValue;

        public AssertionProbe(String exampleNameOrNull, FilePos pos, String expression, boolean isExpectedValue) {
            super(exampleNameOrNull, pos);
            this.expression = expression;
            this.isExpectedValue = isExpectedValue;
        }

        public boolean isExpectedValue() {
            return isExpectedValue;
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            Object result = inlineEvaluator.apply(expression);
            boolean assertionState;
            if (isExpectedValue) {
                assertionState = result.equals(value);
            } else {
                try {
                    assertionState = LIB.isBoolean(result) && LIB.asBoolean(result);
                } catch (UnsupportedMessageException e) {
                    throw CompilerDirectives.shouldNotReachHere(e);
                }
            }
            if (assertionState) {
                return ObjectInformation.create(expression, assertionState);
            } else {
                return ObjectInformation.create(expression, assertionState, String.format("expected: %s; got: %s", result, isExpectedValue ? value : true));
            }
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.ASSERTION;
        }
    }

    public static final class ExampleActive {
        private final int lineNumber;
        private final boolean active;

        public static ExampleActive fromJSON(JSONObject json) {
            try {
                return new ExampleActive(
                        json.getInt("lineNumber"),
                        json.getBoolean("active")
                );
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        public ExampleActive(int lineNumber, boolean active) {
            this.lineNumber = lineNumber;
            this.active = active;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static final class ExampleProbe extends AbstractProbe {
        public static final String EXAMPLE_FILTER_ATTRIBUTE = ":example";
        private static final String EXAMPLE_NAME_ATTRIBUTE = ":name";

        private final String targetIdentifier;
        private final String[] targetArgumentExpressions;
        private final String languageId;

        public ExampleProbe(String line, FilePos pos, String languageId, FunctionDefinition functionDefinition, LinkedHashMap<String, String> attributes) {
            super(attributes.getOrDefault(EXAMPLE_NAME_ATTRIBUTE, fallbackName(line)), pos);
            this.languageId = languageId;
            targetIdentifier = functionDefinition.getIdentifier();
            LinkedHashSet<String> parameters = functionDefinition.getParameters();
            targetArgumentExpressions = new String[parameters.size()];
            int i = 0;
            for (String parameterName : parameters) {
                targetArgumentExpressions[i++] = attributes.get(parameterName);
            }
        }

        private static String fallbackName(String line) {
            return line.substring(line.indexOf(BabylonianAnalysisCommand.EXAMPLE_PREFIX), line.lastIndexOf('>') + 1);
        }

        public String getLanguageId() {
            return languageId;
        }

        public String getTargetIdentifier() {
            return targetIdentifier;
        }

        public String[] getTargetArgumentExpressions() {
            return targetArgumentExpressions;
        }

        public String getInvocationExpression() {
            // TODO: Find better way to determine invocation expression (this is not
            // language-agnostic).
            return String.format("%s(%s)", targetIdentifier, String.join(", ", targetArgumentExpressions));
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            return ObjectInformation.create(section.getCharacters().toString(), value);
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.EXAMPLE;
        }
    }

    public static final class OrphanProbe extends AbstractProbe {
        public OrphanProbe(String exampleNameOrNull, FilePos pos) {
            super(exampleNameOrNull, pos);
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            return ObjectInformation.create(section.getCharacters().toString(), value);
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.ORPHAN;
        }
    }

    public static final class SelectionProbe extends AbstractProbe {
        private final String expression;

        public SelectionProbe(String exampleNameOrNull, String expression, FilePos pos) {
            super(exampleNameOrNull, pos);
            this.expression = expression;
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            return ObjectInformation.create(expression, inlineEvaluator.apply(expression));
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.SELECTION;
        }
    }

    public static final class StatementProbe extends AbstractProbe {
        public StatementProbe(String exampleNameOrNull, FilePos pos) {
            super(exampleNameOrNull, pos);
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            return ObjectInformation.create(section.getCharacters().toString(), value);
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.PROBE;
        }
    }

    public static final class StatementProbeWithExpression extends AbstractProbe {
        public static final String PROBE_EXPRESSION_ATTRIBUTE = ":expression";
        private final String expression;

        public StatementProbeWithExpression(String exampleNameOrNull, FilePos pos, String expression) {
            super(exampleNameOrNull, pos);
            this.expression = expression;
        }

        @Override
        protected ObjectInformation getObjectInformation(ExampleProbe example, SourceSection section, Object value, Function<String, Object> inlineEvaluator) {
            return ObjectInformation.create(expression, inlineEvaluator.apply(expression));
        }

        @Override
        protected ProbeType getProbeType() {
            return ProbeType.PROBE;
        }
    }
}
