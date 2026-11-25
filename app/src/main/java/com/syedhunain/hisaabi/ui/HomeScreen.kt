package com.syedhunain.hisaabi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.KhataItem
import com.syedhunain.hisaabi.util.BannerAd
import com.syedhunain.hisaabi.util.NativeAdRow

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val cur = vm.prefs.currency
    val all by vm.khatas.collectAsState()

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }

    val totalGet = all.filter { it.balance > 0 }.sumOf { it.balance }
    val totalGive = all.filter { it.balance < 0 }.sumOf { -it.balance }

    val qDigits = query.filter { it.isDigit() }
    val shown = all.filter { item ->
        val matchesQuery = query.isBlank() ||
            item.khata.name.contains(query.trim(), ignoreCase = true) ||
            (qDigits.isNotEmpty() && item.khata.phone.filter { it.isDigit() }.contains(qDigits))
        val matchesFilter = when (filter) {
            1 -> item.balance > 0
            2 -> item.balance < 0
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                BannerAd(vm.prefs)
                bottomBar()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = Brand.Emerald,
                contentColor = Brand.Ivory,
                icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Brand.GoldLight) },
                text = { Text(s.newKhata, fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = pad.calculateTopPadding() + 12.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Logo(size = 40.dp, corner = 10.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Hisaabi", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        searching = !searching
                        if (!searching) query = ""
                    }) {
                        Icon(
                            if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searching) s.searchClose else s.searchOpen,
                        )
                    }
                }
            }

            item(key = "search") {
                AnimatedVisibility(
                    visible = searching,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(s.searchHint) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item(key = "summary") {
                SummaryCard(totalGet, totalGive, cur)
            }

            item(key = "filters") {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("${s.filterAll} (${all.size})", s.filterGet, s.filterGive).forEachIndexed { i, label ->
                        FilterChip(
                            selected = filter == i,
                            onClick = { filter = i },
                            label = { Text(label) },
                            shape = RoundedCornerShape(18.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                }
            }

            if (shown.isEmpty()) {
                item(key = "empty") {
                    EmptyState(if (all.isEmpty()) s.emptyHome else s.noResults)
                }
            }

            // Rows in groups of 6, with a native ad between groups (and one at the end of short lists).
            val groups = shown.chunked(6)
            groups.forEachIndexed { g, group ->
                items(group, key = { it.khata.id }) { item ->
                    val i = group.indexOf(item)
                    val index = g * 6 + i
                    Box(Modifier.animateItem()) {
                        StaggerIn(index) {
                            KhataRow(
                                item = item,
                                currency = cur,
                                first = i == 0,
                                last = i == group.lastIndex,
                                onClick = { onOpen(item.khata.id) },
                            )
                        }
                    }
                }
                val lastGroup = g == groups.lastIndex
                if (!lastGroup || shown.size >= 3) {
                    item(key = "native_ad_$g") {
                        NativeAdRow(
                            vm.prefs,
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(totalGet: Long, totalGive: Long, cur: String) {
    val s = LocalStr.current
    val ext = LocalExt.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ext.header)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.youWillGet, fontSize = 13.sp, color = Brand.Mint)
                AnimatedMoney(totalGet, cur, MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), Brand.GoldLight)
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(52.dp)
                    .background(Brand.Ivory.copy(alpha = 0.2f))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.youWillGive, fontSize = 13.sp, color = Brand.Mint)
                AnimatedMoney(totalGive, cur, MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), Brand.Ivory)
            }
        }
        HorizontalDivider(color = Brand.Ivory.copy(alpha = 0.2f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.netBalance, fontSize = 14.sp, color = Brand.Mint, modifier = Modifier.weight(1f))
            val net = totalGet - totalGive
            Text(
                (if (net < 0) "– " else "") + Fmt.money(net, cur),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Brand.Ivory,
            )
        }
    }
}

@Composable
private fun KhataRow(item: KhataItem, currency: String, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val s = LocalStr.current
    val ext = LocalExt.current
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
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(item.khata.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.khata.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    Fmt.relative(item.lastActivity, s),
                    fontSize = 13.sp,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val color = when {
                    item.balance > 0 -> ext.green
                    item.balance < 0 -> ext.red
                    else -> ext.muted
                }
                Text(
                    Fmt.money(item.balance, currency),
                    maxLines = 1,
                    fontSize = if (Fmt.money(item.balance, currency).length >= 12) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    when {
                        item.balance > 0 -> s.filterGet
                        item.balance < 0 -> s.filterGive
                        else -> s.settled
                    },
                    fontSize = 12.sp,
                    color = ext.muted,
                )
            }
        }
        if (!last) HorizontalDivider(color = ext.divider, modifier = Modifier.padding(start = 72.dp))
    }
}
