package io.lucidtech.commons.providers

import android.content.Context
import android.net.Uri
import android.util.Log

import io.lucidtech.commons.annotations.Authority
import io.lucidtech.commons.async.AsyncPromise
import io.lucidtech.commons.async.FutureObserver
import io.lucidtech.commons.async.ObservablePromise
import java.util.concurrent.ConcurrentHashMap


class ContentResolver<T>(context: Context) {
    fun fetch(uri: Uri, autoUpdate: Boolean = false): AsyncPromise<T>? {
        val provider = providers[uri.authority] as? ObservableProvider<T>
        if (provider == null) {
            Log.e(TAG, "No provider registered for authority " + uri.authority)
            return null
        }

        try {
            val promise = provider.fetch(uri) ?: return null

            if (autoUpdate) {
                val observer = object : FutureObserver(uri) {
                    override fun onChange(uri: Uri) {
                        fetch(uri, false)?.let { p -> promise.renew(p) }
                    }
                }

                promise.setObserver(observer)
            }

            return promise
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    fun fetchAll(uri: Uri, autoUpdate: Boolean = false): AsyncPromise<Array<T>>? {
        val provider = providers[uri.authority] as? ObservableProvider<T>
        if (provider == null) {
            Log.e(TAG, "No provider registered for authority " + uri.authority)
            return null
        }

        try {
            val promise = provider.fetchAll(uri)
            val observable = promise as? ObservablePromise<Array<T>> ?: return promise

            if (autoUpdate) {
                val observer = object : FutureObserver(uri) {
                    override fun onChange(uri: Uri) {
                        fetchAll(uri, false)?.let { p -> promise.renew(p) }
                    }
                }

                observable.setObserver(observer)
            }

            return observable
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    fun insert(uri: Uri, obj: T): AsyncPromise<T>? {
        val provider = providers[uri.authority] as? ObservableProvider<T>
        if (provider == null) {
            Log.e(TAG, "No provider registered for authority " + uri.authority)
            return null
        }

        try {
            return provider.insert(uri, obj)
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    fun insert(uri: Uri, objects: List<T>): AsyncPromise<List<T>>? {
        val provider = providers[uri.authority] as ObservableProvider<T>
        try {
            return provider.insert(uri, objects)
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    fun update(uri: Uri, obj: T): AsyncPromise<T>? {
        val provider = providers[uri.authority] as? ObservableProvider<T>

        if (provider == null) {
            Log.e(TAG, "No provider registered for authority " + uri.authority)
            return null
        }

        try {
            return provider.update(uri, obj)
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }

    fun delete(uri: Uri): AsyncPromise<T>? {
        val provider = providers[uri.authority] as? ObservableProvider<T>

        if (provider == null) {
            Log.e(TAG, "No provider registered for authority " + uri.authority)
            return null
        }

        try {
            return provider.delete(uri)
        } catch (e: Exception) {
            Log.e(TAG, Log.getStackTraceString(e))
            return null
        }
    }


    companion object {
        private val TAG = "ContentResolver"

        private val providers = ConcurrentHashMap<String, ObservableProvider<*>>()

        fun clear(uri: Uri) {
            val provider = providers[uri.authority]

            if (provider is CachedProvider<*>) {
                provider.clear(uri)
            }

            provider?.invalidate(uri)
        }

        fun invalidate(uri: Uri) {
            providers[uri.authority]?.invalidate(uri)
        }

        fun clearAll(authority: String) {
            val provider = providers[authority]

            if (provider is CachedProvider<*>) {
                provider.clearAll()
            }
        }

        fun <U> register(provider: ObservableProvider<U>) {
            val authority = provider.wrappedProvider.javaClass.getAnnotation(Authority::class.java)
            providers.put(authority.value, provider)
            Log.d(TAG, "Registered resolver for authority ${authority.value}")
        }
    }
}
