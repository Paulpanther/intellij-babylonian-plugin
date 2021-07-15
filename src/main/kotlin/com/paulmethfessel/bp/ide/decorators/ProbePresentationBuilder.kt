package com.paulmethfessel.bp.ide.decorators

import com.intellij.codeInsight.hints.presentation.*
import com.paulmethfessel.bp.lsp.BpExample
import com.paulmethfessel.bp.lsp.BpProbe

@Suppress("UnstableApiUsage")
class ProbePresentationBuilder(
    private val probe: BpProbe,
    private val factory: PresentationFactory
) {
    private val negativeBiOffset = -8
    private val observedValuesCharLimit = 25
    private val textTopOffset = 5
    private val textLeftOffset = 2
    private val biPresentationHorizontalGap = 8

    fun build(): InlayPresentation {
        val examples = probe.examples
        val presentation = allExamplesPresentation(examples)
        return if (examples.size > 1) {
            factory.inset(presentation, top = negativeBiOffset)
        } else presentation
    }

    private fun allExamplesPresentation(examples: List<BpExample>): InlayPresentation {
        val presentations = mutableListOf<InlayPresentation>()
        for (i in examples.indices step 2) {
            val current = examples[i]
            val next = examples.getOrNull(i + 1)
            presentations += biExamplePresentation(current, next)
        }

        return factory.join(presentations) { SpacePresentation(biPresentationHorizontalGap, 0) }
    }

    private fun biExamplePresentation(example1: BpExample, example2: BpExample?): InlayPresentation {
        val pre1 = exampleToPresentation(example1)
        val pre2 = example2?.let { exampleToPresentation(it) } ?: SpacePresentation(0, 0)
        return VerticalListInlayPresentation(listOf(pre1, pre2))
    }

    private fun exampleToPresentation(example: BpExample): InlayPresentation {
        val values = joinObservedValuesToString(example, observedValuesCharLimit)
        val valuePresentation = factory.inset(factory.text(values), top = textTopOffset, left = textLeftOffset)
        val namePresentation = factory.roundWithBackground(factory.text("\"${example.exampleName}\":"))
        return factory.seq(namePresentation, valuePresentation)
    }
}

fun joinObservedValuesToString(example: BpExample, limit: Int): String {
    val appendix = "..."
    val joined = example.observedValues.joinToString(", ") { it.displayString }
    return if (joined.length > limit) {
        val cut = joined.substring(0, limit - appendix.length)
        return cut + appendix
    } else joined
}
