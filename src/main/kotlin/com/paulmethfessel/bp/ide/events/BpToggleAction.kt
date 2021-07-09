package com.paulmethfessel.bp.ide.events

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.paulmethfessel.bp.ide.services.LSPService
import com.paulmethfessel.bp.ide.services.lsp

class BpToggleAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        if (lsp.connected) {
            lsp.disconnect()
        } else {
//            val file = e.getData(PlatformDataKeys.PSI_FILE) ?: return
            lsp.connect()
//            if (file != null) {
//                lsp.analyze(file)
//            }
        }
    }
}
