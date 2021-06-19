package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.paulmethfessel.bp.ide.services.LSPService
import com.paulmethfessel.bp.lsp.BpRequestProbe

class ChangeHandler: FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val lsp = service<LSPService>()
        lsp.analyze(document, listOf(BpRequestProbe(8, "fibonacci(n - 1) + fibonacci(n - 2)")))
    }
}
