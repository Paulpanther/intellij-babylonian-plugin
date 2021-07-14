package com.paulmethfessel.bp.ide.events

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.FilePos
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.uri

class RemoveLockedProbeAction(
    private val fileUrl: String,
    private val pos: FilePos
): IntentionAction {
    override fun startInWriteAction() = true

    override fun getText() = "Remove probe"
    override fun getFamilyName() = "BpIntentionAction"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        lsp.removeProbeState(fileUrl, pos)
    }
}
