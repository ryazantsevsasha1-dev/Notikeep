package com.notikeep.presentation.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateTimeFormatter = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

/** Today → "HH:mm"; otherwise "d MMM, HH:mm". */
fun formatTimestamp(millis: Long): String {
    val now = System.currentTimeMillis()
    val sameDay = millis / DAY_MS == now / DAY_MS
    val formatter = if (sameDay) timeFormatter else dateTimeFormatter
    return formatter.format(Date(millis))
}

private const val DAY_MS = 24 * 60 * 60 * 1000L
