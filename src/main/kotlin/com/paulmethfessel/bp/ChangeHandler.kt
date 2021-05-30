package com.paulmethfessel.bp

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.DocumentUtil
import com.paulmethfessel.bp.services.LSPService

class ChangeHandler: FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val lsp = service<LSPService>()
        lsp.analyze(document)
    }
}
