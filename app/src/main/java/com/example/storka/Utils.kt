package com.example.storka

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset

fun dateTimeToTimestamp(c: LocalDateTime): Long {
    return c.toEpochSecond(ZoneOffset.UTC) * 1000
}

fun timestampToDateTime(t: Long): LocalDateTime {
    return LocalDateTime.ofEpochSecond(t / 1000, 0, ZoneOffset.UTC)
}