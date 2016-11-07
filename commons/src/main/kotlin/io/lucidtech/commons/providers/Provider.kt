package io.lucidtech.commons.providers

import android.net.Uri
import io.lucidtech.commons.async.ObservablePromise

interface Provider<T, U> {
    fun fetch(uri: Uri): ObservablePromise<T>?
    fun fetchAll(uri: Uri): ObservablePromise<U>?
    fun insert(uri: Uri, obj: T): ObservablePromise<T>?
    fun insert(uri: Uri, objects: List<T>): ObservablePromise<List<T>>?
    fun update(uri: Uri, obj: T): ObservablePromise<T>?
    fun delete(uri: Uri): ObservablePromise<T>?
}
