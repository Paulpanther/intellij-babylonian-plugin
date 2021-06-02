package com.paulmethfessel.bp

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.paulmethfessel.bp.xmlComment.*

class BpAnnotator: Annotator {
    private lateinit var holder: AnnotationHolder

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        this.holder = holder

        if (element !is PsiComment) return
        val comment = CommentParser.tryParse(element) ?: return

        comment.annotate(this)
    }

    fun annotateExample(comment: ExampleComment) {
        val tagRange = comment.tag?.textRange ?: return
        val nameRange = comment.name?.nameElement?.textRange
        val nameValueRange = comment.name?.valueElement?.textRange
        val paramsRange = comment.params.mapNotNull { it?.nameElement?.textRange }
        val paramValuesRange = comment.params.mapNotNull { it?.valueElement?.textRange }

        annotate(tagRange.shiftRight(comment.offset), KEYWORD)
        if (nameRange != null && nameValueRange != null) {
            annotate(nameRange.shiftRight(comment.offset), KEYWORD)
            annotate(nameValueRange.shiftRight(comment.offset), CONSTANT)
        } else {
            annotateError(tagRange.shiftRight(comment.offset), "Missing :name attribute")
        }
        paramsRange.forEach { annotate(it.shiftRight(comment.offset), IDENTIFIER) }
        paramValuesRange.forEach { annotate(it.shiftRight(comment.offset), CONSTANT) }
    }

    fun annotateProbe(comment: ProbeComment) {
        val tagRange = comment.tag?.textRange ?: return
        val nameRange = comment.expression?.nameElement?.textRange
        val nameValueRange = comment.expression?.valueElement?.textRange

        annotate(tagRange.shiftRight(comment.offset), KEYWORD)
        if (nameRange != null && nameValueRange != null) {
            annotate(nameRange.shiftRight(comment.offset), KEYWORD)
            annotate(nameValueRange.shiftRight(comment.offset), CONSTANT)
        }
    }

    private fun annotate(range: TextRange, textAttrib: TextAttributesKey) {
        holder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(textAttrib)
            .create()
    }

    private fun annotateError(range: TextRange, msg: String) {
        holder
            .newAnnotation(HighlightSeverity.ERROR, msg)
            .range(range)
            .create()
    }
}

