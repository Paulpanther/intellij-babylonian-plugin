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

    fun analyze(doc: Document, probes: List<BpRequestProbe>) {
        if (!connected) return

        val projects = ProjectManager.getInstance().openProjects
        val file = projects.map {
            PsiDocumentManager.getInstance(it).getPsiFile(doc)
        }.first { it != null } ?: return

        val currentUri = file.uri.toString()
        val result = lsp.analyze(file.virtualFile.file, probes)
        val lspFile = result.files.find { it.uri == currentUri } ?: return
        _lastProbes[currentUri] = lspFile.probes

        file.virtualFile.refresh(true, true)
    }

    private fun notifyError(title: String, content: String) = notify(title, content, NotificationType.ERROR)
    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        Notifications.Bus.notify(Notification("Bp Notification Group", title, content, type))
    }
}

val VirtualFile.file get() = File(path)
val PsiFile.uri: URI get() = virtualFile.file.toURI()
