package com.rober.blogapp.util

import org.threeten.bp.Instant
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
}