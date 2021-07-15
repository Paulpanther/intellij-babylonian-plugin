package com.paulmethfessel.bp.lsp

sealed class LSPException: Exception()
class AlreadyConnectedException: LSPException()
class ServerConnectionFailedException: LSPException()
class NotConnectedException: LSPException()
class InvalidResponseException: LSPException()
class ServerStartFailedException: LSPException()

