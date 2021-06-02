package com.paulmethfessel.bp.xmlComment

import com.intellij.lang.xml.XMLLanguage
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.paulmethfessel.bp.BpAnnotator
import com.paulmethfessel.bp.ProbeHintsProvider2

object CommentParser {
    fun tryParse(element: PsiElement): CommentElement? {
        val content = element.text.trim('/', '*', ' ')
        val offset = getOffset(element.text, content) + element.textRange.startOffset

        val file = PsiFileFactory.getInstance(element.project)
            .createFileFromText(XMLLanguage.INSTANCE, content)
                as? XmlFile ?: return null
        val tag = file.rootTag ?: return null

        return when {
            ExampleComment.isExample(tag) -> ExampleComment(tag, offset)
            ProbeComment.isProbe(tag) -> ProbeComment(tag, offset)
            else -> null
        }
    }

    private fun getOffset(original: String, newStr: String): Int {
        return original.indexOf(newStr)
    }
}

abstract class CommentElement(
    val root: XmlTag,
    val offset: Int
) {
    val end = offset + root.textRange.endOffset
    abstract fun annotate(annotator: BpAnnotator)
    abstract fun showHint(hinter: ProbeHintsProvider2)
}
