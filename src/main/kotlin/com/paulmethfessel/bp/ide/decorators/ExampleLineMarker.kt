package com.paulmethfessel.bp.ide.decorators

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.paulmethfessel.bp.ide.BabylonianIcons
import com.paulmethfessel.bp.ide.services.LSPService
import com.paulmethfessel.bp.lang.xml.CommentParser
import com.paulmethfessel.bp.lang.xml.ExampleComment
import java.awt.event.MouseEvent

class ExampleLineMarker: LineMarkerProvider {



    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiComment) return null
        val comment = CommentParser.tryParse(element) ?: return null
        if (comment !is ExampleComment) return null

        val onClick = GutterIconNavigationHandler<PsiElement> { _, _ ->
//            val active = comment.state.toggleActive()
//            println(active)
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            BabylonianIcons.Example,
            { "Example" },
            onClick,
            GutterIconRenderer.Alignment.CENTER,
            {"Hello world"})
    }
}
