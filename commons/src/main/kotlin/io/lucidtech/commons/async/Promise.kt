package io.lucidtech.commons.async

interface Promise<T> {
    fun cancel()
    val isDone: Boolean
    val isCancelled: Boolean

    fun onReady(onUiThread: Boolean = true, handler: (T) -> Unit): Promise<T>
    fun onError(onUiThread: Boolean = true, handler: (Exception) -> Unit): Promise<T>
    fun waitForCompletion()
    fun get(): T?
}
