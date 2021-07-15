package com.paulmethfessel.bp.lsp

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

class BpLSPClient: LanguageClient {
    override fun telemetryEvent(obj: Any?) {}
    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}
    override fun showMessage(messageParams: MessageParams?) {}
    override fun showMessageRequest(requestParams: ShowMessageRequestParams?) = CompletableFuture<MessageActionItem>()
    override fun logMessage(message: MessageParams?) {}
}
