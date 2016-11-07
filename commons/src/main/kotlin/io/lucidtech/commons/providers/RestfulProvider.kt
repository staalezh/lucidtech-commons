package com.doplr.providers

import android.net.Uri
import android.util.Log
import com.doplr.annotations.*
import com.doplr.async.ObservablePromise
import io.lucidtech.commons.providers.Provider

import java.lang.reflect.InvocationTargetException

open class RestfulProvider<T, V> : Provider<T, V> {
    class MatchResult(subs: Int, params: Map<String, String>) {
        val substitutions = subs
        val params = Params(params)
    }

    class Params constructor(private val params: Map<String, String>) {
        fun getString(key: String): String? {
            return params[key]
        }
    }

    override fun fetch(uri: Uri): ObservablePromise<T>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(Fetch::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(Fetch::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, match.second.params) as? ObservablePromise<T>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.toString())
                ite.printStackTrace()
            }
        }

        Log.d(TAG, "Could not find fetch handler for uri ${uri.toString()}")
        return null
    }

    override fun fetchAll(uri: Uri): ObservablePromise<V>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(FetchAll::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(FetchAll::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, match.second.params) as? ObservablePromise<V>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.toString())
                ite.printStackTrace()
            }
        }

        Log.d(TAG, "Could not find fetchAll handler for uri ${uri.toString()}")
        return null
    }

    override fun insert(uri: Uri, obj: T): ObservablePromise<T>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(Insert::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(Insert::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, obj, match.second.params) as? ObservablePromise<T>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.message.toString())
                ite.printStackTrace()
            }
        }

        throw IllegalArgumentException("Cannot find insert handler for ${uri.toString()}")
    }

    override fun insert(uri: Uri, objects: List<T>): ObservablePromise<List<T>>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(InsertAll::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(InsertAll::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, objects, match.second.params) as? ObservablePromise<List<T>>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.message.toString())
                ite.printStackTrace()
            }
        }

        throw IllegalArgumentException("Cannot find insertAll handler for ${uri.toString()}")
    }

    override fun update(uri: Uri, obj: T): ObservablePromise<T>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(Update::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(Update::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, obj, match.second.params) as? ObservablePromise<T>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.message.toString())
                ite.printStackTrace()
            }
        }

        throw IllegalArgumentException("Cannot find update handler for ${uri.toString()}")
    }

    override fun delete(uri: Uri): ObservablePromise<T>? {
        val methods = javaClass.methods.filter { m -> m.getAnnotation(Delete::class.java) != null }

        val matches = methods.map { method ->
            val annotation = method.getAnnotation(Delete::class.java)
            val matchExpr = annotation.value
            Pair(method!!, match(uri, matchExpr))
        }

        matches.minBy { p -> p.second.substitutions }?.let { match ->
            try {
                return match.first.invoke(this, uri, match.second.params) as? ObservablePromise<T>
            } catch (ite: InvocationTargetException) {
                Log.e(TAG, ite.message.toString())
                ite.printStackTrace()
            }
        }

        throw IllegalArgumentException("Cannot find delete handler for ${uri.toString()}")
    }

    private fun match(matchUri: Uri, matchExprPath: String): MatchResult {
        val result = mutableMapOf<String, String>()
        val authority = javaClass.getAnnotation(Authority::class.java).value

        val uri = Uri.parse("doplr://$authority$matchExprPath")
        if (uri.pathSegments.size == matchUri.pathSegments.size) {
            val pairs = uri.pathSegments.zip(matchUri.pathSegments).filter {
                p -> p.first != p.second
            }

            var substitutions = 0
            for ((key, value) in pairs) {
                if (key[0] == '<' && key[key.lastIndex] == '>') {
                    result.put(key.substring(1, key.lastIndex), value)
                    ++substitutions
                } else {
                    return MatchResult(Int.MAX_VALUE, result)
                }
            }

            return MatchResult(substitutions, result)
        }

        return MatchResult(Int.MAX_VALUE, result)
    }

    companion object {
        private val TAG = "RestfulProvider"
    }
}
