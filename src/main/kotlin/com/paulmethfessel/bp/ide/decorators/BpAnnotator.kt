package com.paulmethfessel.bp.ide.decorators

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.FileProbeParser
import com.paulmethfessel.bp.ide.document
import com.paulmethfessel.bp.ide.events.RemoveLockedProbeAction
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.uri
import com.paulmethfessel.bp.lang.xml.ExampleComment
import com.paulmethfessel.bp.lang.xml.ProbeComment

class BpAnnotator: Annotator {
    private lateinit var holder: AnnotationHolder

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        this.holder = holder

        FileProbeParser.ifCommentElement(element) { comment ->
            comment.annotate(this)
        }

        if (FileProbeParser.isPossibleProbe(element)) {
            annotatePossibleProbe(element)
        }

        if (element is PsiFile) {
            annotateLockedProbes(element)
        }
    }

    /**
     * This is called once per file
     */
    private fun annotateLockedProbes(file: PsiFile) {
        val uri = file.uri.toString()
        val probes = lsp.getProbeStatesForFile(uri)
        for (probe in probes) {
            val range = probe.pos.toTextRange(file.document)
            holder.newSilentAnnotation(HighlightSeverity.WARNING)
                .highlightType(ProblemHighlightType.WARNING)
                .range(range)
                .withFix(RemoveLockedProbeAction(uri, probe.pos))
                .create()
        }
    }

    private fun annotatePossibleProbe(element: PsiElement) {
        annotate(element.textRange, HIGHLIGHTED_REFERENCE)
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

