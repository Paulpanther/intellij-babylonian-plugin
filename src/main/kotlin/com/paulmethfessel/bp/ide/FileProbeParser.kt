package com.paulmethfessel.bp.ide

import com.intellij.lang.javascript.psi.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.paulmethfessel.bp.lang.xml.CommentElement
import com.paulmethfessel.bp.lang.xml.CommentParser
import com.paulmethfessel.bp.lsp.BpProbe

object FileProbeParser {
    fun ifCommentElement(element: PsiElement, callback: (CommentElement) -> Unit) {
        if (element !is PsiComment) return
        val comment = CommentParser.tryParse(element) ?: return
        callback(comment)
    }

    fun matchProbe(element: PsiElement, probes: List<BpProbe>): BpProbe? {
        return probes.find { probe ->
            probe.pos == element.filePos
        }
    }

    fun isPossibleProbe(element: PsiElement): Boolean {
        val isReference = element.parent is JSReferenceExpression
        val isVariable = element.parent is JSVariable
        val isIdentifier = element.elementType?.toString() == "JS:IDENTIFIER"
        return (isReference || isVariable) && isIdentifier
    }

    fun findProbes(file: PsiFile): List<FilePos> {
        val probes = mutableListOf<FilePos>()
        file.visit { element ->
            if (isPossibleProbe(element)) {
                probes += element.filePos
            }
        }
        return probes
    }

//    fun findExampleActiveInfo(file: PsiFile): List<BpRequestExampleActive> {
//        val exampleActives = mutableListOf<BpRequestExampleActive>()
//        visit(file) { element ->
//            ifCommentElement(element) { comment ->
//                if (comment is ExampleComment) {
//                    exampleActives += BpRequestExampleActive(element.lineNumber, comment.state.active)
//                }
//            }
//        }
//        return exampleActives
//    }
}
