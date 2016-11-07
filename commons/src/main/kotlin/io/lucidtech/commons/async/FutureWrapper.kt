package io.lucidtech.commons.async

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class FutureWrapper<T>(private val result: T) : Future<T> {
    private var isCancelled = false

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        isCancelled = true
        return true
    }

    override fun isCancelled(): Boolean = isCancelled
    override fun isDone(): Boolean = true
    override fun get(): T = result
    override fun get(timeout: Long, unit: TimeUnit): T = result
}
