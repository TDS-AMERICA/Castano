package com.example.castano.utils

import android.content.Context

object ImportStore {
    private const val PREFS = "castano_import_prefs"
    private const val KEY_COUNT = "import_count"
    private const val KEY_URI = "last_import_uri"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCount(ctx: Context): Int = prefs(ctx).getInt(KEY_COUNT, 0)
    fun setCount(ctx: Context, n: Int) {
        prefs(ctx).edit().putInt(KEY_COUNT, n).apply()
    }

    fun getLastUri(ctx: Context): String? = prefs(ctx).getString(KEY_URI, null)
    fun setLastUri(ctx: Context, uri: String) {
        prefs(ctx).edit().putString(KEY_URI, uri).apply()
    }
}