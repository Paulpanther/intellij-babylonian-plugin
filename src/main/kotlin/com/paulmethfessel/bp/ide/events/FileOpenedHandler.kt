package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.propertyBased.PsiIndexConsistencyTester
import com.paulmethfessel.bp.ide.services.lsp

class FileOpenedHandler: FileEditorManagerListener {
    companion object {
        fun register() {
            val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
            project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, FileOpenedHandler())
        }
    }

    override fun fileOpenedSync(
        source: FileEditorManager,
        file: VirtualFile,
        editors: Pair<Array<FileEditor>, Array<FileEditorProvider>>
    ) {
        val psiFile = PsiManager.getInstance(source.project).findFile(file) ?: return
        invokeLater {
            lsp.analyzeForReload(psiFile)
        }
    }
}
