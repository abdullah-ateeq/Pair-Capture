package com.example.paircapture.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Minimal key-value contract so repositories can be unit tested without Android. */
interface PreferenceStore {
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getString(key: String, default: String): String
    fun edit(block: Editor.() -> Unit)
    fun remove(keys: Collection<String>)

    interface Editor {
        fun putInt(key: String, value: Int)
        fun putBoolean(key: String, value: Boolean)
        fun putString(key: String, value: String)
    }
}

class SharedPreferencesStore(private val prefs: SharedPreferences) : PreferenceStore {

    constructor(context: Context, name: String) :
        this(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))

    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getString(key: String, default: String) = prefs.getString(key, default) ?: default

    override fun edit(block: PreferenceStore.Editor.() -> Unit) {
        prefs.edit {
            val target = this
            val editor = object : PreferenceStore.Editor {
                override fun putInt(key: String, value: Int) { target.putInt(key, value) }
                override fun putBoolean(key: String, value: Boolean) { target.putBoolean(key, value) }
                override fun putString(key: String, value: String) { target.putString(key, value) }
            }
            editor.block()
        }
    }

    override fun remove(keys: Collection<String>) {
        prefs.edit { keys.forEach { remove(it) } }
    }
}
