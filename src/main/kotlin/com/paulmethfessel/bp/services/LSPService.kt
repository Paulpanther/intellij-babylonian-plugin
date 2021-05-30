package com.paulmethfessel.bp.services

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.ide.DataManager
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.paulmethfessel.bp.lsp.AlreadyConnectedException
import com.paulmethfessel.bp.lsp.LSPWrapper
import com.paulmethfessel.bp.lsp.ServerConnectionFailedException
import java.io.File

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

    fun analyze(doc: Document) {
        if (!connected) return

        val projects = ProjectManager.getInstance().openProjects
        val file = projects.firstNotNullOfOrNull {
            PsiDocumentManager.getInstance(it).getPsiFile(doc)
        } ?: return


        // TODO use Presentation ?


        val result = lsp.analyze(file.virtualFile.file)
    }

    private fun notifyError(title: String, content: String) = notify(title, content, NotificationType.ERROR)
    private fun notify(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        Notifications.Bus.notify(Notification("Bp Notification Group", title, content, type))
    }

    private val VirtualFile.file get() = File(path)
}
