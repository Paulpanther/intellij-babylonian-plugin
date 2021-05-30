package com.paulmethfessel.bp.xmlComment

import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.XmlUtil

class ProbeComment(
    root: XmlTag
): CommentElement {
    companion object {
        fun isProbe(root: XmlTag) = root.name == "Probe"
    }

    val tag = XmlUtil.getTokenOfType(root, XmlTokenType.XML_NAME)
    val expression = root.getAttribute(":expression")
}
