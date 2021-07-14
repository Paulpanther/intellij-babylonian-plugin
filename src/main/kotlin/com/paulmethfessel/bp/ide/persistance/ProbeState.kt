package com.paulmethfessel.bp.ide.persistance

import com.paulmethfessel.bp.ide.FilePos
import com.paulmethfessel.bp.lsp.BpProbe

data class ProbeState(
    val file: String,
    val pos: FilePos,
    var lastProbeValue: BpProbe?
)
