package com.paulmethfessel.bp.ide.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.FilePos
import com.paulmethfessel.bp.ide.FileProbeParser
import com.paulmethfessel.bp.lsp.*
import java.io.File
import java.net.URI

//@State(name = "com.paulmethfessel.babylonian")
@Service
class LSPService/*: PersistentStateComponent<PluginState>*/ {

//    private var state = PluginState()

    private val lsp = LSPWrapper()

    val connected get() = lsp.connected
    private val _lastProbes = mutableMapOf<String, List<BpProbe>>()
    val lastProbes = _lastProbes as Map<String, List<BpProbe>>

    fun connect() {
        try {
            lsp.connect()
        } catch (e: ServerConnectionFailedException) {
            notifyError("Failed connecting", "Could not connect to language server")
        } catch (e: AlreadyConnectedException) {
            notify("Failed connecting", "Already connected to Language Server")
        }

        notify("Connected to GraalVM", "Connection at ...")
    }

    fun disconnect() {
        lsp.disconnect()
    }

    fun analyze(doc: Document, probe: FilePos): BpProbe? {
        if (!connected) return null

        val file = getFile(doc) ?: return null

        val currentUri = file.uri.toString()
        val result = lsp.analyze(file.virtualFile.file, listOf(probe))
        val lspFile = result.files.find { it.uri == currentUri } ?: return null
        return lspFile.probes.find { it.probeType == "SELECTION" }
    }

    fun analyze(doc: Document) {
        val file = getFile(doc) ?: return
        analyze(file)
    }

    fun analyze(file: PsiFile) {
        val probes = FileProbeParser.findProbes(file)
        val lspFile = analyze(file, probes) ?: return
        _lastProbes[lspFile.uri] = lspFile.probes
    }

    fun analyze(file: PsiFile, probes: List<FilePos>): BpFile? {
        if (!connected) return null

        val currentUri = file.uri.toString()
        val result = lsp.analyze(file.virtualFile.file, probes)
        return result.files.find { it.uri == currentUri }
//        file.virtualFile.refresh(true, true)
    }

    private fun getFile(doc: Document): PsiFile? {
        val projects = ProjectManager.getInstance().openProjects
        return projects.map {
            PsiDocumentManager.getInstance(it).getPsiFile(doc)
        }.firstOrNull { it != null }
    }

    private fun notifyError(title: String, content: String) = notify(title, content, NotificationType.ERROR)
    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        Notifications.Bus.notify(Notification("Bp Notification Group", title, content, type))
    }

//    override fun getState() = state
//
//    override fun loadState(state: PluginState) {
//        // TODO clear unused examples
//        this.state = state
//    }

//    fun getOrCreateExampleState(example: PsiElement): ExampleState {
//        val file = example.containingFile
//        val line = example.lineNumber
//        val uri = file.uri.toString()
//        val existingExample = state.examples.find { it.uri == uri && it.lineNumber == line }
//        if (existingExample != null) return existingExample
//
//        val newExample = ExampleState(uri, line)
//        state.examples += newExample
//        return newExample
//    }
}

val VirtualFile.file get() = File(path)
val PsiFile.uri: URI get() = virtualFile.file.toURI()
