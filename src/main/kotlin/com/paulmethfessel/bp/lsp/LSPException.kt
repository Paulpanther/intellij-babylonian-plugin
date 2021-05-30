package com.paulmethfessel.bp.lsp

open class LSPException: Exception()
class AlreadyConnectedException: LSPException()
class ServerConnectionFailedException: LSPException()
class NotConnectedException: LSPException()
class InvalidResponseException: LSPException()

