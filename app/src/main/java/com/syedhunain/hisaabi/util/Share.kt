package com.syedhunain.hisaabi.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.data.Txn
import com.syedhunain.hisaabi.ui.Fmt
import com.syedhunain.hisaabi.ui.Str
import java.io.File

object Share {

    private fun start(ctx: Context, intent: Intent, s: Str) {
        try {
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, s.noApp, Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(ctx: Context, phone: String, s: Str) {
        start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })), s)
    }

    /** Turns 03001234567 / +923001234567 / 3001234567 into 923001234567 for WhatsApp. */
    fun international(phone: String): String? {
        val d = phone.filter { it.isDigit() }
        return when {
            d.startsWith("92") && d.length >= 12 -> d
            d.startsWith("0") && d.length == 11 -> "92" + d.drop(1)
            d.startsWith("3") && d.length == 10 -> "92$d"
            d.length >= 10 -> d
            else -> null
        }
    }

    fun whatsappReminder(ctx: Context, phone: String, message: String, s: Str) {
        val intl = international(phone)
        if (intl != null) {
            val uri = Uri.parse("https://wa.me/$intl?text=" + Uri.encode(message))
            start(ctx, Intent(Intent.ACTION_VIEW, uri), s)
        } else {
            shareText(ctx, message, s)
        }
    }

    fun shareText(ctx: Context, text: String, s: Str) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(ctx, Intent.createChooser(send, null), s)
    }

    fun shareFile(ctx: Context, file: File, mime: String, s: Str) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(ctx, Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), s)
    }

    private fun sharedDir(ctx: Context): File = File(ctx.cacheDir, "shared").apply { mkdirs() }

    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "khata" }

    /** Builds an A4 PDF statement for one khata (entries oldest first, with running balance). */
    fun statementPdf(
        ctx: Context,
        khata: Khata,
        txns: List<Txn>,
        business: String,
        currency: String,
        s: Str,
    ): File {
        val pageW = 595
        val pageH = 842
        val left = 36f
        val right = pageW - 36f

        val emerald = 0xFF0E5E4E.toInt()
        val gold = 0xFFB08A35.toInt()
        val dark = 0xFF1A1D1F.toInt()
        val grey = 0xFF5F6670.toInt()
        val red = 0xFFC62F35.toInt()
        val green = 0xFF117A4F.toInt()

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f; color = emerald; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = grey }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; color = dark }
        val bold = Paint(body).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val head = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f; color = grey; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line = Paint().apply { color = 0xFFE2E5E1.toInt(); strokeWidth = 1f }
        val goldLine = Paint().apply { color = gold; strokeWidth = 2f }

        fun rightText(c: android.graphics.Canvas, text: String, x: Float, y: Float, p: Paint) {
            c.drawText(text, x - p.measureText(text), y, p)
        }

        fun clip(text: String, maxW: Float, p: Paint): String {
            if (p.measureText(text) <= maxW) return text
            var t = text
            while (t.isNotEmpty() && p.measureText("$t…") > maxW) t = t.dropLast(1)
            return "$t…"
        }

        // Column positions
        val colDate = left
        val colNote = left + 92f
        val colGave = 400f
        val colGot = 475f
        val colBal = right

        val doc = PdfDocument()
        var pageNo = 0
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNo).create())
        var c = page.canvas
        var y: Float

        fun tableHeader(startY: Float): Float {
            var yy = startY
            c.drawText(if (s.romanUrdu) "TAREEKH" else "DATE", colDate, yy, head)
            c.drawText(if (s.romanUrdu) "TAFSEEL" else "DETAILS", colNote, yy, head)
            rightText(c, s.colGave, colGave, yy, head)
            rightText(c, s.colGot, colGot, yy, head)
            rightText(c, if (s.romanUrdu) "BAQI" else "BALANCE", colBal, yy, head)
            yy += 8f
            c.drawLine(left, yy, right, yy, line)
            return yy + 16f
        }

        // Header block
        y = 54f
        c.drawText(business.ifBlank { "Hisaabi" }, left, y, title)
        y += 18f
        c.drawText((if (s.romanUrdu) "Khata statement — " else "Ledger statement — ") + Fmt.date(System.currentTimeMillis()), left, y, sub)
        y += 12f
        c.drawLine(left, y, right, y, goldLine)
        y += 26f
        c.drawText(khata.name, left, y, bold.apply { textSize = 14f })
        bold.textSize = 10.5f
        if (khata.phone.isNotBlank()) {
            y += 15f
            c.drawText(khata.phone, left, y, sub)
        }
        y += 28f
        y = tableHeader(y)

        var running = 0L
        var totalGave = 0L
        var totalGot = 0L
        for (t in txns) {
            if (y > pageH - 70f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNo).create())
                c = page.canvas
                y = tableHeader(54f)
            }
            running += if (t.gave) t.amount else -t.amount
            if (t.gave) totalGave += t.amount else totalGot += t.amount
            val note = when {
                t.isOpening -> s.openingBalance
                t.note.isNotBlank() -> t.note
                else -> "—"
            }
            c.drawText(Fmt.date(t.date), colDate, y, body)
            c.drawText(clip(note, colGave - colNote - 70f, body), colNote, y, body)
            if (t.gave) rightText(c, Fmt.number(t.amount), colGave, y, Paint(body).apply { color = red })
            else rightText(c, Fmt.number(t.amount), colGot, y, Paint(body).apply { color = green })
            rightText(c, (if (running < 0) "-" else "") + Fmt.number(kotlin.math.abs(running)), colBal, y, body)
            y += 8f
            c.drawLine(left, y, right, y, line)
            y += 16f
        }

        if (y > pageH - 120f) {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNo).create())
            c = page.canvas
            y = 54f
        }
        y += 6f
        c.drawText(if (s.romanUrdu) "Kul" else "Total", colNote, y, bold)
        rightText(c, Fmt.number(totalGave), colGave, y, Paint(bold).apply { color = red })
        rightText(c, Fmt.number(totalGot), colGot, y, Paint(bold).apply { color = green })
        y += 30f
        val summary = when {
            running > 0 -> s.willGet(Fmt.money(running, currency))
            running < 0 -> s.willGive(Fmt.money(running, currency))
            else -> s.settled
        }
        val sumPaint = Paint(title).apply { textSize = 16f; color = if (running >= 0) green else red }
        c.drawText(summary, left, y, sumPaint)

        val footer = Paint(sub).apply { textSize = 9f }
        c.drawText("Hisaabi", left, pageH - 30f, footer)
        doc.finishPage(page)

        val file = File(sharedDir(ctx), "Hisaabi_${safeName(khata.name)}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** CSV of every entry, openable in Excel / Google Sheets. */
    fun exportCsv(ctx: Context, khatas: List<Khata>, txns: List<Txn>): File {
        val names = khatas.associate { it.id to it.name }
        val phones = khatas.associate { it.id to it.phone }
        fun q(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        sb.append('﻿') // so Excel reads UTF-8
        sb.append("Date,Khata,Phone,Type,Amount,Note\n")
        for (t in txns) {
            sb.append(q(Fmt.dateTime(t.date))).append(',')
                .append(q(names[t.khataId] ?: "")).append(',')
                .append(q(phones[t.khataId] ?: "")).append(',')
                .append(if (t.isOpening) "Opening" else if (t.gave) "Diya (Gave)" else "Liya (Got)").append(',')
                .append(if (t.gave) t.amount else -t.amount).append(',')
                .append(q(t.note)).append('\n')
        }
        val file = File(sharedDir(ctx), "Hisaabi_export.csv")
        file.writeText(sb.toString())
        return file
    }
}
