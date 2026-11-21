package com.syedhunain.hisaabi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.R
import com.syedhunain.hisaabi.data.Txn
import com.syedhunain.hisaabi.util.Share
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun KhataScreen(
    vm: AppViewModel,
    khataId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEntry: (gave: Boolean, txnId: Long?) -> Unit,
) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cur = vm.prefs.currency

    val items by vm.khatas.collectAsState()
    val item = items.firstOrNull { it.khata.id == khataId } ?: return
    val txns by remember(khataId) { vm.txnsFor(khataId) }.collectAsState(initial = emptyList())
    var askDelete by remember { mutableStateOf(false) }

    val k = item.khata
    val balance = item.balance

    if (askDelete) {
        ConfirmDialog(
            title = s.deleteKhataTitle,
            body = s.deleteKhataBody,
            confirm = s.delete,
            onConfirm = { vm.deleteKhata(k) { onBack() } },
            onDismiss = { askDelete = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(ext.header)
                    .statusBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Brand.Ivory)
                    }
                    Avatar(k.name, size = 40.dp, bg = Brand.Ivory.copy(alpha = 0.15f), fg = Brand.Ivory)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            k.name, color = Brand.Ivory, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            k.phone.ifBlank { s.typeLabel(k.type) },
                            color = Brand.Mint, fontSize = 13.sp,
                        )
                    }
                    if (k.phone.isNotBlank()) {
                        IconButton(onClick = { Share.dial(ctx, k.phone, s) }) {
                            Icon(Icons.Filled.Call, contentDescription = s.call, tint = Brand.Ivory)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = s.edit, tint = Brand.Ivory)
                    }
                    IconButton(onClick = { askDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = s.delete, tint = Brand.Ivory)
                    }
                }
                Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        when {
                            balance > 0 -> s.youWillGet
                            balance < 0 -> s.youWillGive
                            else -> s.settled
                        },
                        color = Brand.Mint, fontSize = 14.sp,
                    )
                    AnimatedMoney(balance, cur, MoneyBig.copy(fontSize = 34.sp), if (balance >= 0) Brand.GoldLight else Color(0xFFFFB4B6))
                }
                Row(
                    Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HeaderAction(
                        label = s.reminder,
                        icon = R.drawable.ic_bell,
                        modifier = Modifier.weight(1f),
                    ) {
                        val msg = s.reminderMessage(k.name, Fmt.money(balance, cur), vm.prefs.business)
                        Share.whatsappReminder(ctx, k.phone, msg, s)
                    }
                    HeaderAction(
                        label = s.pdfStatement,
                        icon = R.drawable.ic_pdf,
                        modifier = Modifier.weight(1f),
                    ) {
                        scope.launch {
                            val list = vm.txnsForOnce(k.id)
                            val file = withContext(Dispatchers.IO) {
                                Share.statementPdf(ctx, k, list, vm.prefs.business, cur, s)
                            }
                            Share.shareFile(ctx, file, "application/pdf", s)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BigActionButton(
                        text = s.gave, color = ext.redButton, up = false,
                        modifier = Modifier.weight(1f),
                    ) { onEntry(true, null) }
                    BigActionButton(
                        text = s.got, color = ext.greenButton, up = true,
                        modifier = Modifier.weight(1f),
                    ) { onEntry(false, null) }
                }
            }
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = pad.calculateTopPadding() + 12.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        ) {
            item(key = "cols") {
                Row(Modifier.padding(horizontal = 32.dp, vertical = 6.dp)) {
                    SectionLabel(s.colEntries, Modifier.weight(1f))
                    ColumnLabel(s.colGave, Modifier.width(88.dp))
                    ColumnLabel(s.colGot, Modifier.width(80.dp))
                }
            }
            if (txns.isEmpty()) {
                item(key = "empty") {
                    Text(
                        s.noEntries,
                        textAlign = TextAlign.Center,
                        color = ext.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                    )
                }
            }
            itemsIndexed(txns, key = { _, t -> t.id }) { index, t ->
                Box(Modifier.animateItem()) {
                    StaggerIn(index) {
                        TxnRow(
                            t = t,
                            first = index == 0,
                            last = index == txns.lastIndex,
                            onClick = { onEntry(t.gave, t.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.6.sp,
    )
}

@Composable
private fun HeaderAction(label: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Brand.Ivory.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .bounceClick(onClick = onClick)
            .height(44.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Brand.GoldLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Brand.Ivory, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BigActionButton(text: String, color: Color, up: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .bounceClick(onClick = onClick)
            .height(56.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = Color.White,
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TxnRow(t: Txn, first: Boolean, last: Boolean, onClick: () -> Unit) {
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
            Modifier
                .fillMaxWidth()
                .bounceClick(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when {
                        t.isOpening -> s.openingBalance
                        t.note.isNotBlank() -> t.note
                        else -> s.noEntry(t.gave)
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(Fmt.dateTime(t.date), fontSize = 12.sp, color = ext.muted)
            }
            Text(
                if (t.gave) Fmt.number(t.amount) else "",
                modifier = Modifier.width(88.dp),
                textAlign = TextAlign.End,
                color = ext.red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (!t.gave) Fmt.number(t.amount) else "",
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End,
                color = ext.green, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        if (!last) HorizontalDivider(color = ext.divider)
    }
}
