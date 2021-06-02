package com.paulmethfessel.bp.xmlComment

import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.XmlUtil
import com.paulmethfessel.bp.BpAnnotator
import com.paulmethfessel.bp.ProbeHintsProvider2

class ProbeComment(
    root: XmlTag,
    offset: Int
): CommentElement(root, offset) {
    companion object {
        fun isProbe(root: XmlTag) = root.name == "Probe"
    }

    val tag = XmlUtil.getTokenOfType(root, XmlTokenType.XML_NAME)
    val expression = root.getAttribute(":expression")

    override fun annotate(annotator: BpAnnotator) = annotator.annotateProbe(this)
    override fun showHint(hinter: ProbeHintsProvider2) = hinter.showHintForProbe(this)
}
