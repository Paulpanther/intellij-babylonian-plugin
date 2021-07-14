package com.paulmethfessel.bp.ide.events

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.uri

class AddLockedProbeAction: IntentionAction {
    override fun startInWriteAction() = true

    override fun getText() = "Lock probe"
    override fun getFamilyName() = "BpIntentionAction"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && editor.selectionModel.hasSelection()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null || !editor.selectionModel.hasSelection()) return
        lsp.createProbeStateFromSelected(file.uri.toString())
    }
}
