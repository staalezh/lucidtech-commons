package io.lucidtech.commons.async

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log

fun executeAsync(body: () -> Unit) {
    object : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try {
                body.invoke()
            } catch (e: Exception) {
                Log.e("executeAsync", Log.getStackTraceString(e))
            }

            return null
        }
    }.execute()
}

fun runOnMain(body: () -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(body)
}

fun <T> observablePromise(context: Context, uri: Uri, body: () -> T?): ObservablePromise<T> {
    return ObservablePromise(context, body, uri)
}

fun <T> asyncPromise(context: Context, body: () -> T?): AsyncPromise<T> {
    return AsyncPromise(context, body)
}

fun <T> asyncPromise(context: Context, result: T): AsyncPromise<T> = AsyncPromise(context, result)

fun <T> whenReady(vararg promises: Promise<T>?, block: (List<T>) -> Unit) {
    fun consume(promises: MutableList<Promise<T>?>, results: MutableList<T>, block: (List<T>) -> Unit) {
        if (promises.isEmpty()) {
            block(results)
        } else {
            val promise = promises.removeAt(0)

            promise?.onReady { res ->
                results.add(res)
                consume(promises, results, block)
            }?.onError { err ->
                consume(promises, results, block)
            } ?: consume(promises, results, block)
        }
    }

    val results = mutableListOf<T>()
    consume(promises.toMutableList(), results, block)
}
