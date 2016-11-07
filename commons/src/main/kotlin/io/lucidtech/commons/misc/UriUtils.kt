package io.lucidtech.commons.misc

import android.net.Uri

object UriUtils {
    fun getParentUri(uri: Uri): Uri {
        val builder = Uri.Builder()
        builder.scheme(uri.scheme)
        builder.authority(uri.authority)

        uri.pathSegments.forEachIndexed { i, segment ->
            if (i != uri.pathSegments.lastIndex) {
                builder.appendPath(segment)
            }
        }

        return builder.build()
    }

}