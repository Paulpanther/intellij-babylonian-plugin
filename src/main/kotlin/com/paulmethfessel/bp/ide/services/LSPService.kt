package com.paulmethfessel.bp.ide.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.FileContentUtil
import com.paulmethfessel.bp.ide.*
import com.paulmethfessel.bp.ide.persistance.ExampleState
import com.paulmethfessel.bp.ide.persistance.PluginState
import com.paulmethfessel.bp.ide.persistance.ProbeState
import com.paulmethfessel.bp.lsp.*

@Service
class LSPService {
    private val lsp = LSPWrapper()

    val connected get() = lsp.connected

    private val _lastProbes = mutableMapOf<String, List<BpProbe>>()
    val lastProbes = _lastProbes as Map<String, List<BpProbe>>

    private val lastCaretSelectionProbes = mutableMapOf<String, BpProbe>()

    private val temporaryState = PluginState()

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
        val probes = getRelevantProbePositions(file)
        val lspFile = analyze(file, probes) ?: return

        updateLockedProbes(file, lspFile.probes)
        updateSelectionProbe(file, lspFile.probes)
        _lastProbes[lspFile.uri] = lspFile.probes

        FileContentUtil.reparseOpenedFiles()
    }

    private fun updateLockedProbes(file: PsiFile, newProbes: List<BpProbe>) {
        val lockedProbes = getProbeStatesForFile(file.uri.toString())
        for (probe in lockedProbes) {
            val selectionProbe = newProbes.find { it.pos == probe.pos }
            selectionProbe?.let {
                it.probeType = BpProbeType.CARET_SELECTION
                probe.lastProbeValue = selectionProbe
            }
        }
    }

    private fun updateSelectionProbe(file: PsiFile, newProbes: List<BpProbe>) {
        val selectionPos = lastCaretSelectionProbes[file.uri.toString()]?.pos
        if (selectionPos != null) {
            val selectionProbe = newProbes.find { it.pos == selectionPos }
            selectionProbe?.let {
                it.probeType = BpProbeType.CARET_SELECTION
                lastCaretSelectionProbes[file.uri.toString()] = selectionProbe
            }
        }
    }

    private fun getRelevantProbePositions(file: PsiFile): List<FilePos> {
        val possibleProbes = FileProbeParser.findProbes(file)
        val selectionPos = lastCaretSelectionProbes[file.uri.toString()]?.pos
        val lockedProbes = getProbeStatesForFile(file.uri.toString()).map { it.pos }

        val probes = (possibleProbes + lockedProbes).toMutableList()
        selectionPos?.let { probes += it }
        return probes
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
        val result = lsp.analyze(file.virtualFile.file, probes, temporaryState.examples)
        if (result.files.isEmpty()) return null
        return result.files.find { it.uri == currentUri }
    }

    private fun notifyError(title: String, content: String) = notify(title, content, NotificationType.ERROR)
    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        Notifications.Bus.notify(Notification("Bp Notification Group", title, content, type))
    }

    fun getOrCreateExampleState(example: PsiElement): ExampleState {
        val file = example.containingFile
        val line = example.lineNumber
        val uri = file.uri.toString()
        val existingExample = temporaryState.examples.find { it.uri == uri && it.lineNumber == line }
        if (existingExample != null) return existingExample

        val newExample = ExampleState(uri, line)
        temporaryState.examples += newExample
        return newExample
    }

    fun createProbeStateFromSelected(file: String): Boolean {
        val selected = lastCaretSelectionProbes[file] ?: return false
        temporaryState.lockedProbes += ProbeState(file, selected.pos, selected)
        FileContentUtil.reparseOpenedFiles()
        return true
    }

    fun getProbeStatesForFile(file: String): List<ProbeState> {
        return temporaryState.lockedProbes.filter { it.file == file }
    }

    fun removeProbeState(file: String, pos: FilePos) {
        temporaryState.lockedProbes.removeIf { it.file == file && it.pos == pos }
        FileContentUtil.reparseOpenedFiles()
    }
}

val lsp get() = service<LSPService>()

