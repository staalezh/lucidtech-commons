package io.lucidtech.commons.async

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WorkerThread(name: String) : HandlerThread(name) {
    lateinit var handler: Handler

    private val lock = ReentrantLock()
    private val looperPrepared = lock.newCondition()
    private var isPrepared: Boolean = false

    init {
        start()
    }

    override fun onLooperPrepared() {
        lock.withLock {
            handler = Handler(looper)
            isPrepared = true
            looperPrepared.signalAll()
        }
    }

    fun waitUntilPrepared() {
        lock.withLock {
        lock.lock()
            if (!isPrepared) {
                looperPrepared.await()
            }
        }
    }
}