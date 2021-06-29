package com.paulmethfessel.bp.ide.persistance

data class ExampleState(
    val uri: String? = null,
    val lineNumber: Int? = null,
    var active: Boolean = false
) {
    fun toggleActive(): Boolean {
        active = !active
        return active
    }
}
