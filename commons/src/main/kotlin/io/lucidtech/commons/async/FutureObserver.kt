package io.lucidtech.commons.async

import android.database.ContentObserver
import android.net.Uri
import android.os.Looper

abstract class FutureObserver(private val uri: Uri) : ContentObserver(android.os.Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, changedUri: Uri?) {
        if (uri.path == changedUri!!.path) {
            onChange(uri)
        }
    }

    protected abstract fun onChange(uri: Uri)
}
