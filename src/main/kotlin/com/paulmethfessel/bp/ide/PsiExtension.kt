package com.paulmethfessel.bp.ide

import com.intellij.lang.javascript.psi.JSRecursiveWalkingElementVisitor
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parents
import java.io.File
import java.net.URI

val PsiElement.document get() = containingFile.viewProvider.document!!

val Document.psiFile get(): PsiFile? {
    val projects = ProjectManager.getInstance().openProjects
    return projects.map {
        PsiDocumentManager.getInstance(it).getPsiFile(this)
    }.firstOrNull { it != null }
}

val PsiElement.lineNumber get() = document.getLineNumber(textOffset)

data class FilePos(val line: Int, val start: Int, val end: Int) {
    fun toTextRange(document: Document): TextRange {
        val lineStart = document.getLineStartOffset(line)
        return TextRange(lineStart + start, lineStart + end)
    }
}

fun TextRange.toFilePos(document: Document): FilePos {
    val lineNumber = document.getLineNumber(startOffset)
    val lineStart = document.getLineStartOffset(lineNumber)
    return FilePos(lineNumber, startOffset - lineStart, endOffset - lineStart)
}

val PsiElement.filePos get() = textRange.toFilePos(document)

fun PsiElement.findCommonParent(other: PsiElement): PsiElement? {
    val myParents = parents(true).toHashSet()
    return other.parents(true).find { myParents.contains(it) }
}

fun PsiFile.visit(visitor: (PsiElement) -> Unit) {
    accept(object: JSRecursiveWalkingElementVisitor() {
        override fun visitElement(element: PsiElement) {
            super.visitElement(element)
            visitor(element)
        }
    })
}

val VirtualFile.file get() = File(path)
val PsiFile.uri: URI get() = virtualFile.file.toURI()

val Caret.selectionRange get() = TextRange(selectionStart, selectionEnd)
val SelectionModel.selectionRange get() = TextRange(selectionStart, selectionEnd)
