package com.example.castano.screens

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Registro(
    val ubicacion: String,
    val codigo: String,          // Código interno (EN203…)
    val pataIzq: String, val pataDer: String,
    val bandejasIzq: String, val bandejasDer: String,
    val unidadIzq: String, val unidadDer: String,
    val cajasIzq: String, val cajasDer: String,
    val total: String,
    val descripcion: String,
    // C. Día
    val wwdt: String? = null,
    val turno: Int? = null,
    val fechaFacturacion: Long? = null,
    val fechaCaptura: Long? = null,
    // EAN
    val ean: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("ubicacion", ubicacion)
        put("codigo", codigo)
        put("pataIzq", pataIzq)
        put("pataDer", pataDer)
        put("bandejasIzq", bandejasIzq)
        put("bandejasDer", bandejasDer)
        put("unidadIzq", unidadIzq)
        put("unidadDer", unidadDer)
        put("cajasIzq", cajasIzq)
        put("cajasDer", cajasDer)
        put("total", total)
        put("descripcion", descripcion)
        put("timestamp", timestamp)
        // campos opcionales: usar putOpt para no borrar la clave con null
        putOpt("wwdt", wwdt)
        putOpt("fechaFacturacion", fechaFacturacion)
        putOpt("turno", turno)
        putOpt("fechaCaptura", fechaCaptura)
        putOpt("ean", ean)
    }

    companion object {
        fun fromJson(o: JSONObject) = Registro(
            ubicacion = o.optString("ubicacion"),
            codigo = o.optString("codigo"),
            pataIzq = o.optString("pataIzq"),
            pataDer = o.optString("pataDer"),
            bandejasIzq = o.optString("bandejasIzq"),
            bandejasDer = o.optString("bandejasDer"),
            unidadIzq = o.optString("unidadIzq"),
            unidadDer = o.optString("unidadDer"),
            cajasIzq = o.optString("cajasIzq"),
            cajasDer = o.optString("cajasDer"),
            total = o.optString("total"),
            descripcion = o.optString("descripcion"),
            timestamp = o.optLong("timestamp"),
            // opcionales con null-safe
            wwdt = o.optString("wwdt").takeIf { it.isNotBlank() },
            fechaFacturacion = if (o.has("fechaFacturacion") && !o.isNull("fechaFacturacion")) o.optLong("fechaFacturacion") else null,
            turno = if (o.has("turno") && !o.isNull("turno")) o.optInt("turno") else null,
            fechaCaptura = if (o.has("fechaCaptura") && !o.isNull("fechaCaptura")) o.optLong("fechaCaptura") else null,
            ean = o.optString("ean").takeIf { it.isNotBlank() }
        )
    }
}

object RegistroStore {
    private const val PREFS = "castano_prefs"
    private const val KEY = "registros_json"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Registro> {
        val json = prefs(ctx).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = mutableListOf<Registro>()
        for (i in 0 until arr.length()) {
            out += Registro.fromJson(arr.getJSONObject(i))
        }
        return out
    }

    private fun save(ctx: Context, lista: List<Registro>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(ctx: Context, reg: Registro) {
        val l = load(ctx)
        l.add(0, reg) // último primero
        save(ctx, l)
    }

    fun clear(ctx: Context) {
        save(ctx, emptyList())
    }
}
