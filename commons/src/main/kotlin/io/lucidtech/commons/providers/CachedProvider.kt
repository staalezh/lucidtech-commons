package io.lucidtech.commons.providers

import android.content.Context
import android.net.Uri
import android.util.Log
import io.lucidtech.commons.async.ObservablePromise
import io.lucidtech.commons.async.observablePromise
import io.lucidtech.commons.cache.Cache
import io.lucidtech.commons.db.Identifiable
import io.lucidtech.commons.misc.UriUtils

import java.lang.reflect.Type


open class CachedProvider<T : Identifiable>(private val context: Context,
                                            wrappedProvider: Provider<T, Array<T>>,
                                            private val fetchType: Type,
                                            private val fetchAllType: Type) : ObservableProvider<T>(context, wrappedProvider) {


    private val objectCache: Cache<T>
    private val fetchAllCache: Cache<Array<T>>

    init {
        val name = wrappedProvider.javaClass.canonicalName
        this.objectCache = Cache<T>(context, name, 50000000)
        this.fetchAllCache = Cache<Array<T>>(context, "${name}_fetchAll", 10000000)
    }

    override fun fetch(uri: Uri): ObservablePromise<T>? = fetch(uri, false)

    private fun fetch(uri: Uri, cacheOnly: Boolean): ObservablePromise<T>? {
        val key = getCacheKey(uri)

        if (objectCache.exists(key) && objectCache[key, fetchType] != null) {
            Log.d(TAG, "[cached] Fetching uri $uri ...")
            return ObservablePromise(context, objectCache[key, fetchType]!!, uri)
        }

        if (cacheOnly) {
            return null
        }

        Log.d(TAG, "Fetching uri $uri ...")
        return super.fetch(uri)?.onReady(false) { result ->
            Log.d(TAG, "Fetched $uri. Caching result...")
            objectCache.put(key, result, fetchType)
        } as ObservablePromise<T>
    }

    override fun fetchAll(uri: Uri): ObservablePromise<Array<T>>? {
        val key        = getCacheKey(uri)
        val skipCache  = uri.getQueryParameter(NO_CACHE)
        val noCache    = YES == skipCache
        val firstQuery = uri.getQueryParameter(BEGIN_MARKER) == null

        if (fetchAllCache.exists(key) && !noCache) {
            Log.d(TAG, "[cached] Fetch all on uri " + uri.toString() + " ...")
            return observablePromise(context, uri) {
                fetchAllCache[key, fetchAllType]!!
            }
        } else {
            Log.d(TAG, "$uri (key: $key) was not in cache...")
        }

        return super.fetchAll(uri)?.onReady(false) { result ->
            val fetchAllKey = getCacheKey(uri, skipMarker = true)

            for (obj in result) {
                objectCache.put(getCacheKey(uri, obj, skipMarker = true), obj, fetchType)
            }

            val newResult = if (noCache || firstQuery) result else
                fetchAllCache[fetchAllKey, fetchAllType]?.plus(result) ?: result

            Log.d(TAG, "Cached ${result.size} objects to $fetchAllKey (total: ${newResult.size})")
            fetchAllCache.put(fetchAllKey, newResult, fetchAllType)
        } as ObservablePromise<Array<T>>
    }

    override fun insert(uri: Uri, obj: T): ObservablePromise<T>? {
        injectCache(uri, obj)
        return super.insert(uri, obj)
    }

    override fun update(uri: Uri, obj: T): ObservablePromise<T>? {
        injectCache(uri, obj, false)
        return super.update(uri, obj)
    }

    override fun insert(uri: Uri, objects: List<T>): ObservablePromise<List<T>>? {
        for (obj in objects) {
            injectCache(uri, obj)
        }

        return super.insert(uri, objects)
    }

    override fun delete(uri: Uri): ObservablePromise<T>? {
        clear(uri)
        clear(UriUtils.getParentUri(uri))
        return super.delete(uri)
    }

    fun clearAll() {
        objectCache.clear()
        fetchAllCache.clear()
    }

    fun clear(uri: Uri) {
        val key = getCacheKey(uri)

        if (objectCache.exists(key)) objectCache.remove(key)
        if (fetchAllCache.exists(key)) fetchAllCache.remove(key)
    }

    protected fun injectCache(uri: Uri, obj: T, create: Boolean = true) {
        val objectUri = if (create) {
            uri.buildUpon().appendPath(obj.id).build()
        } else {
            uri
        }

        val objectKey = getCacheKey(objectUri)
        val fetchAllKey = getCacheKey(uri)

        val objects = fetchAllCache[fetchAllKey, fetchAllType] as? Array<T>
        if (objects != null) {
            val result = objects + obj
            result.sortBy { it.id }
            fetchAllCache.put(fetchAllKey, result, fetchAllType)
        }

        objectCache.put(objectKey, obj, fetchType)
    }

    protected fun getCacheKey(uri: Uri, skipMarker: Boolean = false): String {
        val builder = Uri.Builder()
        builder.scheme(uri.scheme)
        builder.authority(uri.authority)
        builder.path(uri.path)

        val skipParams = listOf(PAGE_SIZE, NO_CACHE) +
                if (skipMarker) listOf(BEGIN_MARKER) else listOf()

        for (query in uri.queryParameterNames) {
            if (!skipParams.contains(query)) {
                builder.appendQueryParameter(query, uri.getQueryParameter(query))
            }
        }

        return builder.build().toString()
    }

    private fun getCacheKey(baseUri: Uri, obj: T, skipMarker: Boolean = false): String {
        val uri = Uri.parse(getCacheKey(baseUri, skipMarker))
        return uri.buildUpon().appendPath(obj.id).build().toString()
    }


    companion object {
        const val PAGE_SIZE = "pageSize"
        const val NO_CACHE = "noCache"
        const val BEGIN_MARKER = "beginMarker"
        const val YES = "yes"

        private val TAG = "CachedProvider"
    }
}
