@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.bp.ide.decorators

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel
import com.paulmethfessel.bp.ProbeIConfigurable
import com.paulmethfessel.bp.ide.FileProbeParser
import com.paulmethfessel.bp.ide.events.FileOpenedHandler
import com.paulmethfessel.bp.ide.events.FileSelectionHandler
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.uri
import com.paulmethfessel.bp.lang.xml.ExampleComment
import com.paulmethfessel.bp.lang.xml.ProbeComment
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
    private lateinit var original: PsiElement

    init {
        FileSelectionHandler.register()
        FileOpenedHandler.register()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ) = object: FactoryInlayHintsCollector(editor) {

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            val hinter = this@ProbeHintsProvider2
            hinter.original = element
            hinter.sink = sink
            hinter.factory = factory

//            if (element !is PsiComment) return true
//            val comment = CommentParser.tryParse(element) ?: return true
//            comment.showHint(hinter)

            val selectionProbe = lsp.getSelectionProbeOfFile(element.containingFile.uri.toString()) ?: return true
            val lineEnd = editor.document.getLineEndOffset(selectionProbe.pos.line)
            val probeText = selectionProbe.examples[0].observedValues.joinToString(", ") { it.displayString }
            val p = factory.inset(factory.text(probeText), top = 6, left = 3)
            sink.addInlineElement(lineEnd, true, p, true)

            return false
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
        val probes = lsp.lastProbes[original.containingFile.uri.toString()] ?: return
        val probe = FileProbeParser.matchProbe(original, probes) ?: return

        val firstExample = probe.examples.getOrNull(0) ?: return
        val probeText = firstExample.observedValues.joinToString(", ") { it.displayString }

        val p = factory.inset(factory.text(probeText), top = 0, left = 3)
        sink.addInlineElement(example.end, true, p, true)
    }
}
