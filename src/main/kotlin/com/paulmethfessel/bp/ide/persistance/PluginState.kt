package com.paulmethfessel.bp.ide.persistance

data class PluginState(
    val examples: MutableList<ExampleState> = mutableListOf()
)
