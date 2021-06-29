package com.paulmethfessel.bp.lsp

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.paulmethfessel.bp.ide.FilePos
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

class LSPWrapper {
    private val port = "3000"

    private var launcher: Launcher<LanguageServer>? = null
    private val gson = Gson()
    private var version = 0

    val connected get() = launcher != null

    @Throws(AlreadyConnectedException::class, ServerConnectionFailedException::class)
    fun connect() {
        if (launcher != null) throw AlreadyConnectedException()

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

    fun disconnect() {
        launcher = null
    }

    @Throws(InvalidResponseException::class, NotConnectedException::class)
    fun analyze(file: File, probes: List<FilePos>): BpResult {
        val params = createBpAnalysisCommandParams(file.toURI(), probes, listOf())
        val launcher = launcher ?: throw NotConnectedException()

        val document = TextDocumentItem(file.toURI().toString(), file.extension, version++, file.contents)
        val identifier = TextDocumentIdentifier(file.toURI().toString())

        launcher.remoteProxy.textDocumentService.didOpen(DidOpenTextDocumentParams(document))
        val result = launcher.remoteProxy.workspaceService.executeCommand(params).get()  // TODO timeout
        launcher.remoteProxy.textDocumentService.didClose(DidCloseTextDocumentParams(identifier))

        try {
            return gson.fromJson(gson.toJson(result), BpResponse::class.java).result
        } catch (e: JsonSyntaxException) {
            throw InvalidResponseException()
        }
    }

    private val File.contents get() = readLines().joinToString("\n")
}
