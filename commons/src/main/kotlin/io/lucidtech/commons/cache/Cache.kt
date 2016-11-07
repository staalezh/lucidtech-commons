package io.lucidtech.commons.cache

import android.content.Context
import android.util.Log
import android.util.LruCache
import io.lucidtech.commons.BuildConfig

import java.io.File
import java.io.IOException
import java.lang.reflect.Type

class Cache<T>(context: Context, name: String, diskSize: Int, memSize: Int = diskSize / 2) {
    private var diskCache: DiskCache? = null
    private val memCache: LruCache<String, T>

    init {
        val appVersion = BuildConfig.VERSION_CODE
        val cacheDir = File("${context.cacheDir}/$name")

        try {
            diskCache = DiskCache.open(diskSize, cacheDir, appVersion)
        } catch (e: IOException) {
            Log.d(TAG, "Could not open DiskCache with name '$name'")
        }

        memCache = LruCache<String, T>(memSize)
    }

    operator fun get(key: String?, type: Type): T? {
        if (key == null) return null

        var item: T? = memCache.get(key)

        if (item == null && diskCache != null && diskCache!!.exists(key)) {
            item = DiskCacheHelper.get<T>(diskCache, key, type)
            if (item != null) {
                memCache.put(key, item)
            }
        }

        return item
    }

    fun put(key: String?, item: T?, type: Type) {
        if (key == null || item == null) return

        memCache.put(key, item)

        if (diskCache != null) {
            DiskCacheHelper.put(diskCache, key, item, type)
        }
    }

    fun remove(key: String?) {
        if (key == null) return

        memCache.remove(key)

        if (diskCache != null) {
            diskCache!!.remove(key)
        }
    }

    fun exists(key: String): Boolean {
        val item = memCache.get(key)
        if (item != null) {
            return true
        }

        if (diskCache != null) {
            return diskCache!!.exists(key)
        }

        return false
    }

    fun clear(memCacheOnly: Boolean = false) {
        memCache.evictAll()

        try {
            if (!memCacheOnly) {
                diskCache?.clear()
            }
        } catch (e: IOException) {
            Log.e(TAG, Log.getStackTraceString(e))
        }
    }

    companion object {
        val TAG = "Cache"
    }
}
