// Purpose: In-memory SharedPreferences fake for host-JVM repository tests
// Inputs: none — backed by a plain MutableMap
// Outputs: same read/write contract as the real SharedPreferences, no Android framework calls
package com.gymtracker.data

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor =
            apply { pending[key!!] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
            apply { pending[key!!] = values }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
            apply { pending[key!!] = value }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            apply { pending[key!!] = value }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            apply { pending[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            apply { pending[key!!] = value }
        override fun remove(key: String?): SharedPreferences.Editor =
            apply { toRemove += key!! }
        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) map.clear()
            toRemove.forEach { map.remove(it) }
            map.putAll(pending)
        }
    }
}
