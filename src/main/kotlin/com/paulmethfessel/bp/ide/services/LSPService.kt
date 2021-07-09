package com.paulmethfessel.bp.ide.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.intellij.util.FileContentUtil
import com.paulmethfessel.bp.ide.*
import com.paulmethfessel.bp.lsp.*

//@State(name = "com.paulmethfessel.babylonian")
@Service
class LSPService/*: PersistentStateComponent<PluginState>*/ {

//    private var state = PluginState()

    private val lsp = LSPWrapper()

    val connected get() = lsp.connected

    private val _lastProbes = mutableMapOf<String, List<BpProbe>>()
    val lastProbes = _lastProbes as Map<String, List<BpProbe>>

    private val lastCaretSelectionProbes = mutableMapOf<String, BpProbe>()

    fun connect(): Boolean {
        try {
            lsp.connect()
            notify("Connected to GraalVM", "Connection at ...")
            return true
        } catch (e: ServerConnectionFailedException) {
            notifyError("Failed connecting", "Could not connect to language server")
        } catch (e: AlreadyConnectedException) {
            notify("Failed connecting", "Already connected to Language Server")
        }
        return false
    }

    fun disconnect() {
        lsp.disconnect()
    }

    fun getSelectionProbeOfFile(uri: String) = lastCaretSelectionProbes[uri]

//    fun analyze(doc: Document, probe: FilePos): BpProbe? {
//        if (!connected) return null
//
//        val file = doc.psiFile ?: return null
//
//        val currentUri = file.uri.toString()
//        val result = lsp.analyze(file.virtualFile.file, listOf(probe))
//        val lspFile = result.files.find { it.uri == currentUri } ?: return null
//        return lspFile.probes.find { it.probeType == "SELECTION" }
//    }

    fun analyzeForReload(file: PsiFile) {
        // find possible probes and current selection
        val probes = FileProbeParser.findProbes(file).toMutableList()
        val selectionPos = lastCaretSelectionProbes[file.uri.toString()]?.pos
        selectionPos?.let { probes += it }

        val lspFile = analyze(file, probes) ?: return

        // Store all probes and also selection
        if (selectionPos != null) {
            val selectionProbe = lspFile.probes.find { it.pos == selectionPos }
            selectionProbe?.let {
                it.probeType = BpProbeType.CARET_SELECTION
                lastCaretSelectionProbes[file.uri.toString()] = selectionProbe
            }
        }
        _lastProbes[lspFile.uri] = lspFile.probes

        FileContentUtil.reparseOpenedFiles()
    }

    fun analyzeCaretSelection(file: PsiFile, selectedProbe: FilePos) {
        val lspFile = analyze(file, listOf(selectedProbe)) ?: return
        val probe = lspFile.probes.find { it.probeType == BpProbeType.SELECTION } ?: return
        probe.probeType = BpProbeType.CARET_SELECTION
        lastCaretSelectionProbes[lspFile.uri] = probe
        FileContentUtil.reparseOpenedFiles()
    }

    private fun analyze(file: PsiFile, probes: List<FilePos>): BpFile? {
        if (!connected) {
            if (!connect()) return null
        }

        val currentUri = file.uri.toString()
        val result = lsp.analyze(file.virtualFile.file, probes)
        if (result.files.isEmpty()) return null
        return result.files.find { it.uri == currentUri }
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

val lsp get() = service<LSPService>()

