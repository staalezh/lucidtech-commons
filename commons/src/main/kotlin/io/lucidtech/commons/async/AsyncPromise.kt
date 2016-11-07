package io.lucidtech.commons.async

import android.content.Context
import android.util.Log
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


open class AsyncPromise<T> : Promise<T> {
    protected val context: Context
    private val lock = ReentrantLock()
    private var task: Future<T?>
    private var error: Exception? = null
    private val handlers = ConcurrentHashMap<(T) -> Unit, Boolean>()
    private val errorHandlers = ConcurrentHashMap<(Exception) -> Unit, Boolean>()

    override var isCancelled: Boolean = false
    override var isDone: Boolean

    var tag: Any? = null

    constructor(context: Context, callable: () -> T?) {
        this.context = context.applicationContext
        this.isDone = false
        this.task = executor.submit(buildTask(callable))
    }

    constructor(context: Context, result: T) {
        this.context = context
        this.task = FutureWrapper(result)
        this.isDone = true
    }

    override fun onReady(onUiThread: Boolean, handler: (T) -> Unit): AsyncPromise<T> {
        handlers[handler] = onUiThread

        if (lock.withLock { isDone && !isCancelled }) {
            task.get()?.let { deliverResult(it, handler, onUiThread) }
        }

        return this
    }

    override fun onError(onUiThread: Boolean, handler: (Exception) -> Unit): AsyncPromise<T> {
        errorHandlers[handler] = onUiThread

        if (lock.withLock { isDone && !isCancelled }) {
            error?.let { deliverResult(it, handler, onUiThread) }
        }

        return this
    }

    fun renew(promise: AsyncPromise<T>) {
        promise.onReady { result ->
            handlers.forEach { h -> deliverResult(result, h.key, h.value) }
        }.onError { err ->
            errorHandlers.forEach { h -> deliverResult(err, h.key, h.value) }
        }
    }

    protected  fun <U> deliverResult(result: U, handler: (U) -> Unit, onUiThread: Boolean) {
        if (lock.withLock { !isCancelled }) {
            if (onUiThread) {
                runOnMain { handler.invoke(result) }
            } else {
                handler.invoke(result)
            }
        }
    }

    override fun waitForCompletion() {
        task.get()
    }

    override fun cancel() {
        lock.withLock {
            isCancelled = true
            isDone = true
        }

        task.cancel(true)
    }

    private fun buildTask(callable: () -> T?): Callable<T?> {
        return Callable {
            try {
                val result: T? = callable()
                lock.withLock { isDone = true }

                if (result != null) {
                    for (handler in handlers.keys) {
                        deliverResult(result, handler, handlers[handler] ?: true)
                    }
                }

                result
            } catch (err: Exception) {
                Log.e(TAG, Log.getStackTraceString(err))

                lock.withLock {
                    error = err
                    isDone = true
                }

                for (handler in errorHandlers.keys) {
                    deliverResult(err, handler, errorHandlers[handler] ?: true)
                }

                null
            }
        }
    }

    override fun get(): T? = task.get()

    companion object {
        private val TAG = "AsyncPromise"
        private val executor = Executors.newFixedThreadPool(20)
    }
}
