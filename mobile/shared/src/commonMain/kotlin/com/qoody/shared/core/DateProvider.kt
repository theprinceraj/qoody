package com.qoody.shared.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Single source of "now" and "today" for the app. Injected everywhere instead of calling
 * [Clock.System] directly so that time-dependent logic stays deterministic in tests.
 */
class DateProvider(
    private val clock: Clock = Clock.System,
    private val zoneProvider: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    val zone: TimeZone get() = zoneProvider()

    fun now(): Instant = clock.now()

    fun today(): LocalDate = now().toLocalDateTime(zone).date
}
