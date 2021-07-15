package com.paulmethfessel.bp.lsp

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.paulmethfessel.bp.ide.FilePos
import com.paulmethfessel.bp.ide.persistance.ExampleState
import com.paulmethfessel.bp.ide.settings.babylonianSettings
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

class LSPWrapper: Disposable {
    private val port = "3000"

    private var process: Process? = null
    private var launcher: Launcher<LanguageServer>? = null
    private val gson = Gson()
    private var version = 0

    val connected get() = launcher != null

    init {
        Disposer.register(babylonianSettings, this)
    }

    @Throws(AlreadyConnectedException::class, ServerConnectionFailedException::class, ServerStartFailedException::class)
    fun connect() {
        if (launcher != null) throw AlreadyConnectedException()

        startServerProcess()
        Thread.sleep(2000)

        val processBuilder = ProcessBuilder()
        processBuilder.command("nc", "localhost", port)

        val process: Process
        try {
            process = processBuilder.start()
        } catch (e: Exception) {
            throw ServerConnectionFailedException()
        }

        val launcher = LSPLauncher.createClientLauncher(BpLSPClient(), process.inputStream, process.outputStream)
        launcher.startListening()
        launcher.remoteProxy.initialize(InitializeParams()).get(5000, TimeUnit.MILLISECONDS)

        this.launcher = launcher
    }

    @Throws(ServerStartFailedException::class)
    private fun startServerProcess() {
        val settings = babylonianSettings

        val debugArgs = "--vm.Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
        val args = "./polyglot --lsp=$port --jvm --experimental-options --shell".split(" ").toMutableList()
        if (settings.startWithDebugger) args += debugArgs

        val processBuilder = ProcessBuilder()
        processBuilder.directory(File(settings.graalPath))
        processBuilder.command(args)
        try {
            process = processBuilder.start()
        } catch (e: Exception) {
            throw ServerStartFailedException()
        }
    }

    fun disconnect() {
        launcher = null
        println("Destroyed")
        process?.destroyForcibly()
    }

    @Throws(InvalidResponseException::class, NotConnectedException::class)
    fun analyze(file: File, probes: List<FilePos>, exampleStates: List<ExampleState>): BpResult {
        val exampleRefs = exampleStates.mapNotNull { example ->
            example.lineNumber?.let {
                BpRequestExampleActive(it + 1, example.active)
            }
        }
        val params = createBpAnalysisCommandParams(file.toURI(), probes, exampleRefs)
        val launcher = launcher ?: throw NotConnectedException()

        val document = TextDocumentItem(file.toURI().toString(), file.extension, version++, file.contents)
        val identifier = TextDocumentIdentifier(file.toURI().toString())

        launcher.remoteProxy.textDocumentService.didOpen(DidOpenTextDocumentParams(document))
        val result = launcher.remoteProxy.workspaceService.executeCommand(params).get() ?: throw InvalidResponseException()  // TODO timeout
        launcher.remoteProxy.textDocumentService.didClose(DidCloseTextDocumentParams(identifier))

        try {
            return gson.fromJson(gson.toJson(result), BpResponse::class.java).result
        } catch (e: JsonSyntaxException) {
            throw InvalidResponseException()
        }
    }

    private val File.contents get() = readLines().joinToString("\n")

    override fun dispose() = disconnect()
}
