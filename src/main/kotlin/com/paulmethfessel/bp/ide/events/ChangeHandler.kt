package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.paulmethfessel.bp.ide.services.lsp

class ChangeHandler: FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        lsp.analyze(document)
    }
}
