package com.paulmethfessel.bp.xmlComment

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomElement
import com.intellij.util.xml.DomManager

object CommentParser {
    fun tryParse(project: Project, comment: String): CommentWrapper? {
        val content = comment.trim('/', '*', ' ')
        val offset = getOffset(comment, content)

        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(XMLLanguage.INSTANCE, content)
                as? XmlFile ?: return null
        val tag = file.rootTag ?: return null

        return when {
            ExampleComment.isExample(tag) -> CommentWrapper(offset, ExampleComment(tag))
            ProbeComment.isProbe(tag) -> CommentWrapper(offset, ProbeComment(tag))
            else -> null
        }
    }

    private fun getOffset(original: String, newStr: String): Int {
        return original.indexOf(newStr)
    }
}

interface CommentElement

class CommentWrapper(
    val offset: Int,
    val elem: CommentElement)
