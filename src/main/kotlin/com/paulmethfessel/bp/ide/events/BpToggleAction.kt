package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.paulmethfessel.bp.ide.services.LSPService

class BpToggleAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val lsp = service<LSPService>()
        if (lsp.connected) {
            lsp.disconnect()
        } else {
            lsp.connect()
//            val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
//            if (file != null) {
//                lsp.analyze(file)
//            }
        }
    }
}
