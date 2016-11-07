package io.lucidtech.commons.db

import android.content.Context
import kotlin.reflect.KProperty

open class Keystore(private val context: Context, private val name: String = Keystore.DEFAULT) {
    companion object { const val DEFAULT = "default" }

    private val keystore = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val editor = keystore.edit()

    inner class KeystoreDelegate<T>(private val field: String, private val default: T) {
        private val error = "Invalid type. Valid types are String, Int, Boolean, Float"

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return keystore.all[field] as? T ?: default
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            when (value) {
                is String  -> editor.putString(field, value)
                is Int     -> editor.putInt(field, value)
                is Boolean -> editor.putBoolean(field, value)
                is Float   -> editor.putFloat(field, value)
                is Long    -> editor.putLong(field, value)
                is Set<*>  -> editor.putStringSet(field, value as Set<String>)
                else       -> throw IllegalArgumentException(error)
            }


            editor.commit()
        }
    }

    inner class Wrapper<T>(property: String, default: T) {
        var value: T by KeystoreDelegate(property, default)
    }

    fun <T> with(property: String, default: T, block: (Wrapper<T>) -> Unit) {
        block(Wrapper(property, default))
    }

    fun <T> get(property: String, default: T): T {
        return Wrapper(property, default).value
    }

    fun <T> set(property: String, value: T) {
        Wrapper(property, value).value = value
    }
}