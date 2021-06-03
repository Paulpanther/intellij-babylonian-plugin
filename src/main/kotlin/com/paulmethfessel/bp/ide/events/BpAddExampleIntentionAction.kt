package com.paulmethfessel.bp.ide.events

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.impl.JSPsiElementFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class BpAddExampleIntentionAction: IntentionAction {

    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getText() = "Add example to function"

    override fun getFamilyName() = "BpIntentionAction"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return tryGetFunction(editor, file) != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val function = tryGetFunction(editor, file) ?: return
        val parameterStr = function.parameterVariables.joinToString(" ") { it.name + "=\"\"" }
        val comment = JSPsiElementFactory.createPsiComment("// <Example :name=\"\" $parameterStr />", function)
        function.addBefore(comment, function.firstChild)
    }

    private fun tryGetFunction(editor: Editor?, file: PsiFile?): JSFunction? {
        if (editor == null || file == null) return null
        val offset = editor.caretModel.offset
        val currentElement = file.findElementAt(offset) ?: return null
        val parent = currentElement.parent
        return parent as? JSFunction
    }
}
