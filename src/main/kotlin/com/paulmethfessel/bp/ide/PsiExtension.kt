package com.paulmethfessel.bp.ide

import com.intellij.lang.javascript.psi.JSRecursiveWalkingElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

private val PsiElement.document get() = containingFile.viewProvider.document!!

val PsiElement.lineNumber get() = document.getLineNumber(textOffset)

data class FilePos(val line: Int, val start: Int, val end: Int)
val PsiElement.filePos get(): FilePos {
    val lineStart = document.getLineStartOffset(lineNumber)
    return FilePos(lineNumber, textRange.startOffset - lineStart, textRange.endOffset - lineStart)
}

fun PsiFile.visit(visitor: (PsiElement) -> Unit) {
    accept(object: JSRecursiveWalkingElementVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            visitor(element)
        }
    })
}
