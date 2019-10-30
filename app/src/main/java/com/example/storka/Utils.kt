package com.example.storka

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

fun dateTimeToTimestamp(c: LocalDateTime): Long {
    return c.toEpochSecond(ZoneOffset.UTC) * 1000
}

fun timestampToDateTime(t: Long): LocalDateTime {
    return LocalDateTime.ofEpochSecond(t / 1000, 0, ZoneOffset.UTC)
}

fun formatTime(dateTime: LocalDateTime?): String {
    return dateTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
}