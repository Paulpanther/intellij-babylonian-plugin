package com.paulmethfessel.bp.ide.decorators

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.paulmethfessel.bp.ide.services.LSPService
import com.paulmethfessel.bp.ide.services.uri
import com.paulmethfessel.bp.lsp.BpProbe
import com.paulmethfessel.bp.lsp.BpRequestProbe

class BpDocumentationProvider: AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val lsp = service<LSPService>()

        val currentUri = element?.containingFile?.uri?.toString() ?: return null
        val probes = lsp.lastProbes[currentUri] ?: return null
        val probe = findMatchingProbe(element, probes) ?: return null

        return probe.examples[0].observedValues.joinToString(", ")
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return requestSingleProbeInfo(element)
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

    private fun getCachedSingleProbeInfo(element: PsiElement): String? {
        val lsp = service<LSPService>()

        val currentUri = element.containingFile?.uri?.toString() ?: return null
        val probes = lsp.lastProbes[currentUri] ?: return null
        val probe = findMatchingProbe(element, probes) ?: return null

        return probe.examples[0].observedValues.joinToString(", ")
    }

    private fun requestSingleProbeInfo(element: PsiElement): String? {
        val lsp = service<LSPService>()

        val line = element.lineNumber ?: return null
        val lspFile = lsp.analyze(element.containingFile, listOf(BpRequestProbe(line + 1, element.text))) ?: return null
        val probe = lspFile.probes.firstOrNull { it.probeType == "SELECTION" } ?: return null
        return probe.examples[0].observedValues.joinToString(", ") { it.displayString }
    }

    private fun findMatchingProbe(element: PsiElement, probes: List<BpProbe>): BpProbe? {
        val probesInLine = probes.filter {
            it.lineIndex + 1 == element.lineNumber && it.probeType == "SELECTION"
        }
        return probesInLine.firstOrNull()
    }
}
