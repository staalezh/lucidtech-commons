package io.lucidtech.commons.providers

import android.content.Context
import android.net.Uri
import io.lucidtech.commons.async.ObservablePromise
import io.lucidtech.commons.misc.UriUtils


open class ObservableProvider<T>(private val context: Context, val wrappedProvider: Provider<T, Array<T>>) : Provider<T, Array<T>> {
    override fun fetch(uri: Uri): ObservablePromise<T>? = wrappedProvider.fetch(uri)
    override fun fetchAll(uri: Uri): ObservablePromise<Array<T>>? = wrappedProvider.fetchAll(uri)

    override fun insert(uri: Uri, obj: T): ObservablePromise<T>? {
        try {
            return wrappedProvider.insert(uri, obj)
        } finally {
            invalidate(uri)
        }
    }

    override fun update(uri: Uri, obj: T): ObservablePromise<T>? {
        try {
            return wrappedProvider.update(uri, obj)
        } finally {
            invalidate(uri)
        }
    }

    override fun insert(uri: Uri, objects: List<T>): ObservablePromise<List<T>>? {
        try {
            return wrappedProvider.insert(uri, objects)
        } finally {
            invalidate(uri)
        }
    }

    override fun delete(uri: Uri): ObservablePromise<T>? {
        try {
            return wrappedProvider.delete(uri)
        } finally {
            invalidate(UriUtils.getParentUri(uri))
        }
    }

    open fun invalidate(uri: Uri) {
        context.contentResolver.notifyChange(uri, null)
    }
}

