package com.paulmethfessel.bp.lsp

import org.eclipse.lsp4j.*
import java.net.URI

fun createBpAnalysisCommandParams(uri: URI, probes: List<BpRequestProbe>) =
    ExecuteCommandParams("babylonian_analysis", listOf(uri.toString(), probes))

class BpRequestProbe(val line: Int, val expression: String) {
//    val json: String = Gson().toJson(this)
}

class BpResponse(val result: BpResult)
class BpResult(val files: List<BpFile>)
class BpFile(val languageId: String, val probes: List<BpProbe>, val uri: String)
class BpProbe(val examples: List<BpExample>, val probeType: String, val lineIndex: Int)
class BpExample(val observedValues: List<BpObservedValue>, val exampleName: String)
class BpObservedValue(val expression: String, val interopProperties: List<String>, val displayString: String)
