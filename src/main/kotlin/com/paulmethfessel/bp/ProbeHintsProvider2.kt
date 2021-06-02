@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.bp

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel
import com.intellij.ui.paint.EffectPainter
import com.paulmethfessel.bp.xmlComment.CommentParser
import com.paulmethfessel.bp.xmlComment.ExampleComment
import com.paulmethfessel.bp.xmlComment.ProbeComment
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

class JavaPanelHelper {
    fun createPanel(): JComponent = panel {}
}

class ProbeHintsProvider2: InlayHintsProvider<NoSettings> {
    override val key = SettingsKey<NoSettings>("blabla")
    override val name = "probe inline hints"
    override val previewText = "loading probes"

    override fun createConfigurable(settings: NoSettings) = ProbeIConfigurable()
    override fun createSettings() = NoSettings()

    private lateinit var sink: InlayHintsSink
    private lateinit var factory: PresentationFactory

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ) = object: FactoryInlayHintsCollector(editor) {
        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            val hinter = this@ProbeHintsProvider2
            hinter.sink = sink
            hinter.factory = factory

            if (element !is PsiComment) return true
            val comment = CommentParser.tryParse(element) ?: return true
            comment.showHint(hinter)

            return true
        }
    }

    fun showHintForExample(example: ExampleComment) {
        val p = factory.text("Example")
        sink.addBlockElement(example.end,
            relatesToPrecedingText = true,
            showAbove = true,
            priority = BlockInlayPriority.CODE_VISION,
            presentation = p
        )
    }

    fun showHintForProbe(example: ProbeComment) {
        val p = factory.inset(factory.text("Probe"), top = 0, left = 3)
        sink.addInlineElement(example.end, true, p, true)
    }
}
