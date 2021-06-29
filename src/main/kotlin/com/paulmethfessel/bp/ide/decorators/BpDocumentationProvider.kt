package com.paulmethfessel.bp.ide.decorators

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.paulmethfessel.bp.ide.FileProbeParser
import com.paulmethfessel.bp.ide.filePos
import com.paulmethfessel.bp.ide.lineNumber
import com.paulmethfessel.bp.ide.services.LSPService
import com.paulmethfessel.bp.ide.services.uri
import com.paulmethfessel.bp.lsp.BpProbe

class BpDocumentationProvider: AbstractDocumentationProvider() {
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return element?.let { getCachedSingleProbeInfo(it) }
    }

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return getCachedSingleProbeInfo(element)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement != null && FileProbeParser.isPossibleProbe(contextElement)) {
            return contextElement.parent
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }

    private fun getCachedSingleProbeInfo(element: PsiElement): String? {
        val lsp = service<LSPService>()

        val currentUri = element.containingFile?.uri?.toString() ?: return null
        val probes = lsp.lastProbes[currentUri] ?: return null
        val probe = FileProbeParser.matchProbe(element, probes) ?: return null

        return probe.examples[0].observedValues.joinToString(", ") { it.displayString }
    }

    private fun requestSingleProbeInfo(element: PsiElement): String? {
        val lsp = service<LSPService>()

        val lspFile = lsp.analyze(element.containingFile, listOf(element.filePos)) ?: return null
        val probe = lspFile.probes.firstOrNull { it.probeType == "SELECTION" } ?: return null
        return probe.examples[0].observedValues.joinToString(", ") { it.displayString }
    }
}
