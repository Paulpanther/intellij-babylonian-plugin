package com.paulmethfessel.bp

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.XmlRecursiveElementWalkingVisitor
import com.intellij.psi.xml.XmlAttribute
import com.paulmethfessel.bp.xmlComment.*

// https://plugins.jetbrains.com/docs/intellij/annotator.html#register-the-annotator
class ProbeAnnotator: Annotator {
    private lateinit var holder: AnnotationHolder

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        this.holder = holder

        if (element !is PsiComment) return
        val comment = CommentParser.tryParse(element.project, element.text) ?: return

        val globalOffset = element.textRange.startOffset
        when (comment.elem) {
            is ExampleComment -> annotateExample(comment.offset + globalOffset, comment.elem)
            is ProbeComment -> annotateProbe(comment.offset + globalOffset, comment.elem)
        }
    }

    private fun annotateExample(offset: Int, exampleComment: ExampleComment) {
        val tagRange = exampleComment.tag?.textRange ?: return
        val nameRange = exampleComment.name?.nameElement?.textRange
        val nameValueRange = exampleComment.name?.valueElement?.textRange
        val paramsRange = exampleComment.params.mapNotNull { it?.nameElement?.textRange }
        val paramValuesRange = exampleComment.params.mapNotNull { it?.valueElement?.textRange }

        annotate(tagRange.shiftRight(offset), KEYWORD)
        if (nameRange != null && nameValueRange != null) {
            annotate(nameRange.shiftRight(offset), KEYWORD)
            annotate(nameValueRange.shiftRight(offset), CONSTANT)
        } else {
            annotateError(tagRange.shiftRight(offset), "Missing :name attribute")
        }
        paramsRange.forEach { annotate(it.shiftRight(offset), IDENTIFIER) }
        paramValuesRange.forEach { annotate(it.shiftRight(offset), CONSTANT) }
    }

    private fun annotateProbe(offset: Int, probeComment: ProbeComment) {
        val tagRange = probeComment.tag?.textRange ?: return
        val nameRange = probeComment.expression?.nameElement?.textRange
        val nameValueRange = probeComment.expression?.valueElement?.textRange

        annotate(tagRange.shiftRight(offset), KEYWORD)
        if (nameRange != null && nameValueRange != null) {
            annotate(nameRange.shiftRight(offset), KEYWORD)
            annotate(nameValueRange.shiftRight(offset), CONSTANT)
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

