package com.paulmethfessel.bp.xmlComment

import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.XmlUtil
import com.paulmethfessel.bp.BpAnnotator
import com.paulmethfessel.bp.ProbeHintsProvider2

class ExampleComment(
    root: XmlTag,
    offset: Int
): CommentElement(root, offset) {
    companion object {
        fun isExample(root: XmlTag) = root.name == "Example"
    }

    val tag = XmlUtil.getTokenOfType(root, XmlTokenType.XML_NAME)
    val name = root.getAttribute(":name")
    val params = root.attributes.filter { it.name != ":name" }

    override fun annotate(annotator: BpAnnotator) = annotator.annotateExample(this)
    override fun showHint(hinter: ProbeHintsProvider2) = hinter.showHintForExample(this)
}
