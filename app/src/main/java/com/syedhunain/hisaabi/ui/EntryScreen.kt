package com.syedhunain.hisaabi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun EntryScreen(
    vm: AppViewModel,
    khataId: Long,
    initialGave: Boolean,
    txnId: Long?,
    onBack: () -> Unit,
) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val cur = vm.prefs.currency

    val items by vm.khatas.collectAsState()
    val item = items.firstOrNull { it.khata.id == khataId } ?: return
    val txns by remember(khataId) { vm.txnsFor(khataId) }.collectAsState(initial = emptyList())
    val existing = txnId?.let { id -> txns.firstOrNull { it.id == id } }

    var gave by rememberSaveable { mutableStateOf(initialGave) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var loaded by rememberSaveable { mutableStateOf(txnId == null) }
    var showError by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!loaded && existing != null) {
            gave = existing.gave
            amountText = existing.amount.toString()
            note = existing.note
            date = existing.date
            loaded = true
        }
    }

    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (txnId == null) {
            delay(300)
            runCatching { focus.requestFocus() }
        }
    }

    LaunchedEffect(saved) {
        if (saved) {
            delay(900)
            onBack()
        }
    }

    val amount = amountText.toLongOrNull() ?: 0L
    val accent = if (gave) ext.red else ext.green
    val accentButton = if (gave) ext.redButton else ext.greenButton

    // Balance preview: remove the old version of this entry, add the new one.
    val oldEffect = existing?.let { if (it.gave) it.amount else -it.amount } ?: 0L
    val newEffect = if (gave) amount else -amount
    val after = item.balance - oldEffect + newEffect

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { sel ->
                        val day = Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate()
                        val time = Fmt.zdt(date).toLocalTime()
                        date = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showPicker = false
                }) { Text(s.ok) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(s.cancel) } },
        ) {
            DatePicker(state = state)
        }
    }

    if (askDelete && existing != null) {
        ConfirmDialog(
            title = s.deleteEntryTitle,
            body = null,
            confirm = s.delete,
            onConfirm = {
                vm.deleteTxn(existing)
                onBack()
            },
            onDismiss = { askDelete = false },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, contentDescription = s.close) }
                Column(Modifier.weight(1f)) {
                    Text(if (gave) s.gave else s.got, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (gave) s.gaveTo(item.khata.name) else s.gotFrom(item.khata.name),
                        fontSize = 13.sp, color = ext.muted,
                    )
                }
                if (existing != null) {
                    IconButton(onClick = { askDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = s.delete, tint = ext.red)
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                // Gave / Got toggle
                Row(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToggleChoice(s.gave, gave, ext.red, ext.redSoft, Modifier.weight(1f)) { gave = true }
                    ToggleChoice(s.got, !gave, ext.green, ext.greenSoft, Modifier.weight(1f)) { gave = false }
                }

                // Amount
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(s.amount, fontSize = 13.sp, color = ext.muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cur, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = ext.muted)
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = amountText,
                            onValueChange = { v ->
                                amountText = v.filter { it.isDigit() }.take(11).trimStart('0')
                                showError = false
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold, color = accent),
                            cursorBrush = SolidColor(accent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(220.dp)
                                .focusRequester(focus),
                            decorationBox = { inner ->
                                Box {
                                    if (amountText.isEmpty()) {
                                        Text("0", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = accent.copy(alpha = 0.3f))
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    Box(
                        Modifier
                            .width(240.dp)
                            .height(2.dp)
                            .background(accent)
                    )
                    if (amount > 0) {
                        Text(Fmt.money(amount, cur), fontSize = 13.sp, color = ext.muted)
                    }
                    if (showError) {
                        Text(s.amountError, fontSize = 13.sp, color = ext.red)
                    }
                }

                // Details card
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showPicker = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(s.date, fontSize = 12.sp, color = ext.muted)
                            Text(Fmt.dateTime(date), fontSize = 15.sp)
                        }
                    }
                    HorizontalDivider(color = ext.divider)
                    Row(
                        Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(80) },
                            label = { Text(s.noteOptional) },
                            placeholder = { Text(s.noteHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                }

                // Balance after
                Row(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.goldSoft)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.balanceAfter, fontSize = 14.sp, color = ext.onGoldSoft, modifier = Modifier.weight(1f))
                    Text(
                        when {
                            after > 0 -> s.willGet(Fmt.money(after, cur))
                            after < 0 -> s.willGive(Fmt.money(after, cur))
                            else -> s.settled
                        },
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // Save
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.Emerald)
                    .bounceClick {
                        if (amount <= 0) {
                            showError = true
                        } else if (!saved) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            vm.saveTxn(existing, khataId, amount, gave, note, date)
                            saved = true
                        }
                    }
                    .height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Brand.GoldLight)
                Spacer(Modifier.width(10.dp))
                Text(s.save, color = Brand.Ivory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Saved popup: dim overlay behind, card with an animated tick in front.
        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .animateEnterExit(
                            enter = scaleIn(
                                initialScale = 0.6f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                            ) + fadeIn(),
                        )
                        .width(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(accentButton),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                    Text(
                        s.saved,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        Fmt.money(amount, cur),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleChoice(
    label: String,
    selected: Boolean,
    color: Color,
    soft: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) soft else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outline, shape)
            .bounceClick(onClick = onClick)
            .height(46.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
        )
    }
}
