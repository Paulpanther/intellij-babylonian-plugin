package com.paulmethfessel.bp.lsp

import com.google.gson.Gson
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.net.URI

object BpAnalysis {

}

class BpAnalysisCommandParams(uri: URI, optionals: List<Any> = listOf())
: ExecuteCommandParams("babylonian_analysis", listOf(uri.toString()) + optionals) {
    constructor(uri: URI, lineIndex: Int, expression: String): this(uri, listOf(lineIndex, expression))
}

class BpResponse(val result: BpResult)
class BpResult(val files: List<BpFile>)
class BpFile(val languageId: String, val probes: List<BpProbe>, val uri: String)
class BpProbe(val examples: List<BpExample>, val probeType: String, val lineIndex: Int)
class BpExample(val observedValues: List<BpObservedValue>, val exampleName: String)
class BpObservedValue(val expression: String, val interopProperties: List<String>, val displayString: String)
