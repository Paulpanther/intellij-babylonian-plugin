package com.paulmethfessel.bp.ide.decorators

import com.paulmethfessel.bp.lsp.BpExample
import com.paulmethfessel.bp.lsp.BpProbe

class ProbeDocumentationBuilder(private val probe: BpProbe) {
    private val observedValuesCharLimit = 30

    fun build(): String {
        return probe.examples.joinToString("<br>") { exampleDoc(it) }
    }

    private fun exampleDoc(example: BpExample): String {
        val values = joinObservedValuesToString(example, observedValuesCharLimit)
        return "<strong>${example.exampleName}:</strong> $values"
    }
}
