package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.paulmethfessel.bp.ide.services.LSPService

class ChangeHandler: FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val lsp = service<LSPService>()
        lsp.analyze(document)
    }
}
