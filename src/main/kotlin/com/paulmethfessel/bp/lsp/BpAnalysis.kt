package com.paulmethfessel.bp.lsp

import com.paulmethfessel.bp.ide.FilePos
import org.eclipse.lsp4j.*
import java.net.URI

fun createBpAnalysisCommandParams(
    uri: URI,
    probes: List<FilePos>,
    exampleActives: List<BpRequestExampleActive>) =
    ExecuteCommandParams("babylonian_analysis", listOf(uri.toString(), probes, exampleActives))

data class BpRequestExampleActive(val lineNumber: Int, val active: Boolean)

data class BpResponse(val result: BpResult)
data class BpResult(val files: List<BpFile>)
data class BpFile(val languageId: String, val probes: List<BpProbe>, val uri: String)
data class BpProbe(val examples: List<BpExample>, val probeType: String, val pos: FilePos)
data class BpExample(val observedValues: List<BpObservedValue>, val exampleName: String)
data class BpObservedValue(val expression: String, val interopProperties: List<String>, val displayString: String)
