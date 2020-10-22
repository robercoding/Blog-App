package com.rober.blogapp.util

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Utils {

    fun getDifferenceTimeMilliseconds(beforeTime: Long, isSeconds: Boolean): String {
        var beforeTimeMilliseconds = beforeTime
        if (isSeconds) {
            beforeTimeMilliseconds = Instant.ofEpochSecond(beforeTime).toEpochMilli()
        }
        val nowMilliseconds = Instant.now().toEpochMilli()

        val diffMilliseconds = abs(nowMilliseconds - beforeTimeMilliseconds)

        val diffDays = TimeUnit.DAYS.convert(diffMilliseconds, TimeUnit.MILLISECONDS).toInt()

        when (diffDays) {
            in 1..364 -> {
                return "${diffDays}d"
            }

            0 -> {
                val diffHours = TimeUnit.HOURS.convert(diffMilliseconds, TimeUnit.MILLISECONDS).toInt()
                if (diffHours > 0) {
                    return "${diffHours}h"
                }
                val diffMinutes = TimeUnit.MINUTES.convert(diffMilliseconds, TimeUnit.MILLISECONDS).toInt()
                if (diffMinutes > 0) {
                    return "${diffMinutes}m"
                }
                val diffSeconds = TimeUnit.SECONDS.convert(diffMilliseconds, TimeUnit.MILLISECONDS).toInt()
                return "${diffSeconds}s "
            }

            else -> {
                val diffDays = TimeUnit.DAYS.convert(diffMilliseconds, TimeUnit.MILLISECONDS).toInt()
                return "${diffDays / 365}y"
            }
        }
    }

    fun getDateDayMonthYearInSeconds(seconds: Long): String {
        val instantDate = Instant.ofEpochSecond(seconds)
        val zdt = ZoneId.systemDefault()
        val instantDateZoneId = instantDate.atZone(ZoneId.of(zdt.toString()))

        val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yy")

        return fmtDate.format(instantDateZoneId)
    }

    fun getDateHourMinutesInSeconds(seconds: Long): String {
        val instantDate = Instant.ofEpochSecond(seconds)

        val zdt = ZoneId.systemDefault()
        val instantDateZoneId = instantDate.atZone(ZoneId.of(zdt.toString()))

        val fmtTime = DateTimeFormatter.ofPattern("HH:mm")
        return fmtTime.format(instantDateZoneId)
    }
}