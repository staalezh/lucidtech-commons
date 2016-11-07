package com.doplr.async

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

import java.io.File

class ImageLoader private constructor(private val context: Context) {
    private var imageFile: File? = null
    private var circularCrop: Boolean = false

    fun load(imageFile: File): ImageLoader {
        this.imageFile = imageFile
        return this
    }

    fun withCircularCrop(circularCrop: Boolean): ImageLoader {
        this.circularCrop = circularCrop
        return this
    }

    fun into(imageView: ImageView) {
        try {
            val request = Glide.with(context).load(Uri.fromFile(imageFile)).diskCacheStrategy(DiskCacheStrategy.NONE)

            request.into(imageView)
        } catch (iae: IllegalArgumentException) {
            Log.d(TAG, iae.toString())
            iae.printStackTrace()
        }

    }

    companion object {
        private val TAG = ImageLoader::class.java.toString()

        fun with(context: Context): ImageLoader {
            return ImageLoader(context)
        }

        fun clear(imageView: ImageView) {
            Glide.clear(imageView)
        }
    }
}
