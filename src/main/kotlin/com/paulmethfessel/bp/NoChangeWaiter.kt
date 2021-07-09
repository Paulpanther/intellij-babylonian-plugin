package com.paulmethfessel.bp

import java.util.*
import kotlin.concurrent.schedule

class NoChangeWaiter<T>(
    private val delay: Long,
    private val onNoChange: (T) -> Unit
) {
    private val timer = Timer("NoChangeWaiter", false)
    private var lastTime = System.nanoTime()

    fun change(changeArgs: T) {
        val myTime = System.nanoTime()
        lastTime = myTime
        timer.schedule(delay) {
            if (myTime == lastTime) {
                onNoChange(changeArgs)
            }
        }
    }
}
