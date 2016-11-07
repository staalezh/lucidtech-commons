package io.lucidtech.commons.async

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.util.Log
import io.lucidtech.commons.async.runOnMain
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class ObservablePromise<T> : AsyncPromise<T> {
    private var observer: ContentObserver? = null
    var uri: Uri

    constructor(context: Context, callable: () -> T?, uri: Uri) : super(context, callable) {
        this.uri = uri
        this.tag = uri.toString()
    }

    constructor(context: Context, result: T, uri: Uri) : super(context, result) {
        this.uri = uri
        this.tag = uri.toString()
    }

    override fun onReady(onUiThread: Boolean, handler: (T) -> Unit): ObservablePromise<T> {
        return super.onReady(onUiThread, handler) as ObservablePromise<T>
    }

    fun setObserver(observer: ContentObserver) {
        if (this.observer != null) {
            context.contentResolver.unregisterContentObserver(this.observer!!)
        }

        this.observer = observer

        val builder = Uri.Builder()
        builder.scheme(uri.scheme)
        builder.authority(uri.authority)
        builder.path(uri.path)

        context.contentResolver.registerContentObserver(builder.build(), false, observer)
        Log.d(TAG, "Registered content observer for uri " + builder.build().toString())
    }

    override fun cancel() {
        super.cancel()

        if (observer != null) {
            context.contentResolver.unregisterContentObserver(observer!!)
        }
    }

    @Suppress("unused")
    protected fun finalize() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    companion object {
        private val TAG = "ObservablePromise"
    }
}
