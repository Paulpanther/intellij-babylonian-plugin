package com.paulmethfessel.bp.ide.decorators

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class BpDocumentationProvider: AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        element.toString()
        return "jey"
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return "Hello World"
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement?.parent is JSReferenceExpression) {
            return contextElement.parent
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }
}
