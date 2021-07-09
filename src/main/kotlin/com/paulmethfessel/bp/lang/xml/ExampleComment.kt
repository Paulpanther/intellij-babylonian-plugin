package com.paulmethfessel.bp.lang.xml

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.XmlUtil
import com.paulmethfessel.bp.ide.decorators.BpAnnotator
import com.paulmethfessel.bp.ide.decorators.ProbeHintsProvider2
import com.paulmethfessel.bp.ide.persistance.ExampleState
import com.paulmethfessel.bp.ide.services.LSPService

class ExampleComment(
    val original: PsiElement,
    root: XmlTag,
    offset: Int
): CommentElement(root, offset) {
    companion object {
        fun isExample(root: XmlTag) = root.name == "Example"
    }

    val tag = XmlUtil.getTokenOfType(root, XmlTokenType.XML_NAME)
    val name = root.getAttribute(":name")
    val params = root.attributes.filter { it.name != ":name" }

//    val state = lsp.getOrCreateExampleState(original)

    override fun annotate(annotator: BpAnnotator) = annotator.annotateExample(this)
    override fun showHint(hinter: ProbeHintsProvider2) = hinter.showHintForExample(this)
}
