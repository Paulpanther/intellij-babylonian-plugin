package com.paulmethfessel.bp.ide.services

import com.intellij.lang.javascript.psi.JSRecursiveWalkingElementVisitor
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.decorators.lineNumber
import com.paulmethfessel.bp.lsp.*
import java.io.File
import java.net.URI

@Service
class LSPService {

    private val lsp = LSPWrapper()

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

    val connected get() = lsp.connected
    private val _lastProbes = mutableMapOf<String, List<BpProbe>>()
    val lastProbes = _lastProbes as Map<String, List<BpProbe>>

    fun analyze(doc: Document, probe: BpRequestProbe): BpProbe? {
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
        val probes = findAllProbes(file)
        val lspFile = analyze(file, probes) ?: return
        _lastProbes[lspFile.uri] = offsetProbes(lspFile.probes)
    }

    fun analyze(file: PsiFile, probes: List<BpRequestProbe>): BpFile? {
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

    private fun offsetProbes(probes: List<BpProbe>): List<BpProbe> {
        val sortedProbes = probes.sortedBy { it.lineIndex }
        var offset = 0
        return sortedProbes.map {
            if (it.probeType == "SELECTION") {
                BpProbe(it.examples, it.probeType, it.lineIndex - offset-- + 1)
            } else BpProbe(it.examples, it.probeType, it.lineIndex + 1)
        }
    }

    private fun findAllProbes(file: PsiFile): List<BpRequestProbe> {
        val probes = mutableListOf<BpRequestProbe>()
        file.accept(object: JSRecursiveWalkingElementVisitor() {
            override fun visitJSReferenceExpression(node: JSReferenceExpression?) {
                val line = node?.lineNumber ?: return
                probes += BpRequestProbe(line, node.text)
            }
        })
        return probes
    }

    private fun notifyError(title: String, content: String) = notify(title, content, NotificationType.ERROR)
    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        Notifications.Bus.notify(Notification("Bp Notification Group", title, content, type))
    }
}

val VirtualFile.file get() = File(path)
val PsiFile.uri: URI get() = virtualFile.file.toURI()
