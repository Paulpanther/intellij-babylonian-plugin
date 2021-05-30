package com.paulmethfessel.bp.xmlComment

import com.intellij.psi.impl.source.xml.XmlTokenImpl
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.xml.util.XmlUtil

class ExampleComment(
    root: XmlTag
): CommentElement {
    companion object {
        fun isExample(root: XmlTag) = root.name == "Example"
    }

    val tag = XmlUtil.getTokenOfType(root, XmlTokenType.XML_NAME)
    val name = root.getAttribute(":name")
    val params = root.attributes.filter { it.name != ":name" }
}
