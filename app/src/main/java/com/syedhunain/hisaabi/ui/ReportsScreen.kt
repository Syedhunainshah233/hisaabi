package com.syedhunain.hisaabi.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.data.Txn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max

private data class Period(val start: Long, val end: Long, val label: String)

private fun periodFor(kind: Int, offset: Int): Period {
    val zone = ZoneId.systemDefault()
    fun ms(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
    val today = LocalDate.now()
    val dayF = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    val shortF = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val monthF = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    return when (kind) {
        0 -> {
            val d = today.plusDays(offset.toLong())
            Period(ms(d), ms(d.plusDays(1)), d.format(dayF))
        }
        1 -> {
            val st = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
            val en = st.plusDays(6)
            Period(ms(st), ms(en.plusDays(1)), "${st.format(shortF)} – ${en.format(shortF)}")
        }
        else -> {
            val ym = YearMonth.now().plusMonths(offset.toLong())
            Period(ms(ym.atDay(1)), ms(ym.plusMonths(1).atDay(1)), ym.atDay(1).format(monthF))
        }
    }
}

private data class MonthBar(val label: String, val inAmt: Long, val outAmt: Long)

private fun lastSixMonths(txns: List<Txn>): List<MonthBar> {
    val zone = ZoneId.systemDefault()
    val f = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    val now = YearMonth.now()
    return (5 downTo 0).map { back ->
        val ym = now.minusMonths(back.toLong())
        val st = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val en = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val inRange = txns.filter { !it.isOpening && it.date >= st && it.date < en }
        MonthBar(
            label = ym.atDay(1).format(f),
            inAmt = inRange.filter { !it.gave }.sumOf { it.amount },
            outAmt = inRange.filter { it.gave }.sumOf { it.amount },
        )
    }
}

@Composable
fun ReportsScreen(vm: AppViewModel, onOpenKhata: (Long) -> Unit, bottomBar: @Composable () -> Unit) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val cur = vm.prefs.currency
    val txns by vm.txns.collectAsState()
    val khatas by vm.khatas.collectAsState()
    val names = khatas.associate { it.khata.id to it.khata.name }

    var kind by rememberSaveable { mutableIntStateOf(2) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    val period = periodFor(kind, offset)
    val inPeriod = txns.filter { !it.isOpening && it.date >= period.start && it.date < period.end }
    val cameIn = inPeriod.filter { !it.gave }.sumOf { it.amount }
    val wentOut = inPeriod.filter { it.gave }.sumOf { it.amount }
    val net = cameIn - wentOut
    val bars = lastSixMonths(txns)

    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = bottomBar) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = pad.calculateTopPadding() + 16.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(s.tabReports, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item {
                Segmented(
                    options = listOf(s.periodDay, s.periodWeek, s.periodMonth),
                    selected = kind,
                    onSelect = { kind = it; offset = 0 },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
            item {
                Row(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { offset -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = s.previous)
                    }
                    AnimatedContent(
                        targetState = period.label,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        modifier = Modifier.weight(1f),
                        label = "period",
                    ) { label ->
                        Text(
                            label, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconButton(onClick = { offset += 1 }, enabled = offset < 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = s.next)
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(s.cameIn, cameIn, cur, ext.green, Modifier.weight(1f))
                    StatCard(s.wentOut, wentOut, cur, ext.red, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.header)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.periodNet, color = Brand.Mint, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(
                        (if (net < 0) "– " else "") + Fmt.money(net, cur),
                        color = Brand.GoldLight, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            item { ChartCard(bars) }
            item {
                SectionLabel(s.periodEntries, Modifier.padding(start = 20.dp, top = 6.dp))
            }
            if (inPeriod.isEmpty()) {
                item {
                    Text(
                        s.noPeriodEntries, color = ext.muted, textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                }
            }
            itemsIndexed(inPeriod, key = { _, t -> t.id }) { index, t ->
                val first = index == 0
                val last = index == inPeriod.lastIndex
                val shape = RoundedCornerShape(
                    topStart = if (first) 16.dp else 0.dp, topEnd = if (first) 16.dp else 0.dp,
                    bottomStart = if (last) 16.dp else 0.dp, bottomEnd = if (last) 16.dp else 0.dp,
                )
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .bounceClick { onOpenKhata(t.khataId) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                names[t.khataId] ?: "—", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (if (t.note.isNotBlank()) t.note + " · " else "") + Fmt.dateTime(t.date),
                                fontSize = 12.sp, color = ext.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            (if (t.gave) "− " else "+ ") + Fmt.money(t.amount, cur),
                            color = if (t.gave) ext.red else ext.green,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        )
                    }
                    if (!last) HorizontalDivider(color = ext.divider)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: Long, cur: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val ext = LocalExt.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, fontSize = 13.sp, color = ext.muted)
        AnimatedMoney(value, cur, MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color)
    }
}

@Composable
private fun ChartCard(bars: List<MonthBar>) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val inColor = ext.green
    val outColor = ext.red
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.lastSixMonths, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Legend(s.legendIn, inColor)
            Spacer(Modifier.width(12.dp))
            Legend(s.legendOut, outColor)
        }
        val maxV = max(1L, bars.maxOf { max(it.inAmt, it.outAmt) }).toFloat()
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val slot = size.width / bars.size
            val bw = 12.dp.toPx()
            val gap = 3.dp.toPx()
            val r = CornerRadius(4.dp.toPx())
            bars.forEachIndexed { i, b ->
                val cx = slot * i + slot / 2
                val hIn = size.height * (b.inAmt / maxV) * progress.value
                val hOut = size.height * (b.outAmt / maxV) * progress.value
                drawRoundRect(inColor, topLeft = Offset(cx - bw - gap / 2, size.height - hIn), size = Size(bw, hIn), cornerRadius = r)
                drawRoundRect(outColor, topLeft = Offset(cx + gap / 2, size.height - hOut), size = Size(bw, hOut), cornerRadius = r)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bars.forEach { b ->
                Text(b.label, fontSize = 11.sp, color = ext.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = LocalExt.current.muted)
    }
}
