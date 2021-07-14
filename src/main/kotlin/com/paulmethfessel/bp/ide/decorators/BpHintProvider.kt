@file:Suppress("UnstableApiUsage")

package com.paulmethfessel.bp.ide.decorators

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.BlockInlayPriority
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel
import com.paulmethfessel.bp.ProbeIConfigurable
import com.paulmethfessel.bp.ide.FileProbeParser
import com.paulmethfessel.bp.ide.events.FileOpenedHandler
import com.paulmethfessel.bp.ide.events.FileSelectionHandler
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.uri
import com.paulmethfessel.bp.lang.xml.CommentParser
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

            if (tryShowCommentHint(element)) return true

            if (element is PsiFile && tryShowSelectionProbeHint(element)) return true

            return true
        }

        private fun tryShowCommentHint(element: PsiElement): Boolean {
            if (element !is PsiComment) return false
            val comment = CommentParser.tryParse(element) ?: return false
            comment.showHint(this@ProbeHintsProvider2)
            return true
        }

        private fun tryShowSelectionProbeHint(element: PsiElement): Boolean {
            val selectionProbe = lsp.getSelectionProbeOfFile(element.containingFile.uri.toString()) ?: return false
            val lineEnd = editor.document.getLineEndOffset(selectionProbe.pos.line)
            if (selectionProbe.examples.isNotEmpty()) {
                val probeText = selectionProbe.examples[0].observedValues.joinToString(", ") { it.displayString }
                val p = textPresentation(probeText)
                sink.addInlineElement(lineEnd, true, p, true)
            }
            return true
        }
    }

    fun showHintForExample(example: ExampleComment) {
        val active = example.state.active
        val text = textPresentation("active=\"$active\"")
        sink.addInlineElement(example.end, true, text, true)
    }

    @Deprecated("ProbeComment will be removed")
    fun showHintForProbe(example: ProbeComment) {
        val probes = lsp.lastProbes[original.containingFile.uri.toString()] ?: return
        val probe = FileProbeParser.matchProbe(original, probes) ?: return

        val firstExample = probe.examples.getOrNull(0) ?: return
        val probeText = firstExample.observedValues.joinToString(", ") { it.displayString }

        sink.addInlineElement(example.end, true, textPresentation(probeText), true)
    }

    private fun textPresentation(text: String) = factory.inset(factory.text(text), top = 6, left = 3)
}
