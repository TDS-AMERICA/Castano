package com.example.castano.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone

/** Turno por hora actual: 2=[07:00,15:00), 3=[15:00,22:00), 1=resto. */
fun calcularTurnoAutomatico(): Int {
    val cal = Calendar.getInstance()
    val totalMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val m7 = 7 * 60; val m15 = 15 * 60; val m22 = 22 * 60
    return when {
        totalMin in m7 until m15 -> 2
        totalMin in m15 until m22 -> 3
        else -> 1
    }
}

/** Código WWDT con semana ISO (lunes=1, primera semana con >=4 días). */
fun codigoSemanaDiaTurno(
    fechaMillis: Long,
    turno: Int,
    tz: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault()
): String {
    val cal = Calendar.getInstance(tz, locale).apply {
        timeInMillis = fechaMillis
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    val dayIso = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }
    return String.format(Locale.US, "%02d%d%d", week, dayIso, turno)
}