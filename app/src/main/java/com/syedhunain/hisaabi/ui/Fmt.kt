package com.syedhunain.hisaabi.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

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
    private val shortDateTimeF = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)

    fun dateTime(ms: Long): String = zdt(ms).format(dateTimeF)

    /** "21 Nov, 7:05 AM" this year, full date otherwise. */
    fun dateTimeShort(ms: Long): String {
        val z = zdt(ms)
        return if (z.year == LocalDate.now().year) z.format(shortDateTimeF) else z.format(dateTimeF)
    }

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

/** Shows typed digits with thousands commas ("2000" -> "2,000") while keeping the raw value. */
class ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val value = raw.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val out = Fmt.number(value)
        val o2t = IntArray(raw.length + 1)
        var digits = 0
        for (j in out.indices) {
            if (out[j].isDigit()) {
                digits++
                if (digits <= raw.length) o2t[digits] = j + 1
            }
        }
        val t2o = IntArray(out.length + 1)
        digits = 0
        for (j in 0..out.length) {
            t2o[j] = digits.coerceAtMost(raw.length)
            if (j < out.length && out[j].isDigit()) digits++
        }
        return TransformedText(AnnotatedString(out), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = o2t[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int): Int = t2o[offset.coerceIn(0, out.length)]
        })
    }
}
