package com.syedhunain.hisaabi.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Fmt {
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
    private val dateF = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dateTimeF = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    private val shortF = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val timeF = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** "Rs 12,300" — always positive; the colour/label tells the direction. */
    fun money(value: Long, currency: String): String = "$currency ${nf.format(abs(value))}"

    fun number(value: Long): String = nf.format(value)

    fun zdt(ms: Long): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun date(ms: Long): String = zdt(ms).format(dateF)
    fun dateTime(ms: Long): String = zdt(ms).format(dateTimeF)

    fun relative(ms: Long, s: Str): String {
        val z = zdt(ms)
        val d = z.toLocalDate()
        val today = LocalDate.now()
        return when (d) {
            today -> "${s.today}, ${z.format(timeF)}"
            today.minusDays(1) -> "${s.yesterday}, ${z.format(timeF)}"
            else -> if (d.year == today.year) z.format(shortF) else z.format(dateF)
        }
    }

    fun initials(name: String): String =
        name.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}
