package com.paulmethfessel.bp

import com.intellij.codeInsight.hints.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ProbeHintsProvider: InlayParameterHintsProvider {
    override fun getDefaultBlackList(): MutableSet<String> {
        return mutableSetOf()
    }

    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        return mutableListOf(InlayInfo("hello you", 2, false))
    }

    override fun getParameterHints(element: PsiElement, file: PsiFile): MutableList<InlayInfo> {
        return getParameterHints(element)
    }

    override fun getHintInfo(element: PsiElement): HintInfo? {
        return HintInfo.MethodInfo("foo", listOf("bar"))
    }

    override fun getHintInfo(element: PsiElement, file: PsiFile): HintInfo? {
        return getHintInfo(element)
    }
}
