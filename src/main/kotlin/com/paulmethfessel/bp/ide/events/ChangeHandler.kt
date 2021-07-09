package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.paulmethfessel.bp.ide.psiFile
import com.paulmethfessel.bp.ide.services.lsp

class ChangeHandler: FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        reload(document)
    }

    override fun fileContentLoaded(file: VirtualFile, document: Document) {
//        if (file.fileSystem.protocol == "file") {
//            reload(document)
//        }
    }

    private fun reload(document: Document) {
        invokeLater {
            document.psiFile?.let { lsp.analyzeForReload(it) }
        }
    }
}
