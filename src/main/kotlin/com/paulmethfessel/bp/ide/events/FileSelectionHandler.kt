package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.util.TextRange
import com.paulmethfessel.bp.NoChangeWaiter
import com.paulmethfessel.bp.ide.psiFile
import com.paulmethfessel.bp.ide.services.lsp
import com.paulmethfessel.bp.ide.toFilePos

private data class ChangeArgs(
    val editor: Editor,
    val caret: Caret)

class FileSelectionHandler: CaretListener {
    companion object {
        fun register() {
            val multicaster = EditorFactory.getInstance().eventMulticaster
            multicaster.addCaretListener(FileSelectionHandler(), ApplicationManager.getApplication())
        }
    }

    private val noChangeWaiter = NoChangeWaiter(1000, this::onNoChange)

    override fun caretPositionChanged(event: CaretEvent) {
        val caret = event.caret ?: return
        if (caret.hasSelection()) {
            noChangeWaiter.change(ChangeArgs(event.editor, caret))
        }
    }

    private fun onNoChange(changeArgs: ChangeArgs) {
        invokeLater {
            val (editor, caret) = changeArgs
            val file = editor.document.psiFile

            if (file != null) {
                val filePos = caret.selectionRange.toFilePos(editor.document)
                if (filePos.start != filePos.end) {
                    lsp.analyzeCaretSelection(file, filePos)
                }
            }
        }
    }

//    private fun selectedElement(file: PsiFile, selection: TextRange): PsiElement? {
//        val startElement = file.findElementAt(selection.startOffset)
//        val endElement = file.findElementAt(selection.endOffset)
//        return if (startElement != null && endElement != null) {
//            startElement.findCommonParent(endElement)
//        } else null
//    }
}

private val Caret.selectionRange get() = TextRange(selectionStart, selectionEnd)
