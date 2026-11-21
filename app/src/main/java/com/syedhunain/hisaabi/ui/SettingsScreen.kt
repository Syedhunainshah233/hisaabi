package com.syedhunain.hisaabi.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.R
import com.syedhunain.hisaabi.util.Share
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(vm: AppViewModel, bottomBar: @Composable () -> Unit) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = vm.prefs

    var editBusiness by remember { mutableStateOf(false) }
    var editCurrency by remember { mutableStateOf(false) }
    var setPin by remember { mutableStateOf(false) }

    if (editBusiness) {
        TextInputDialog(
            title = s.businessName, initial = prefs.business, hint = s.businessHint,
            onSave = { prefs.updateBusiness(it) }, onDismiss = { editBusiness = false },
        )
    }
    if (editCurrency) {
        TextInputDialog(
            title = s.currency, initial = prefs.currency,
            onSave = { prefs.updateCurrency(it.take(6)) }, onDismiss = { editCurrency = false },
        )
    }
    if (setPin) {
        SetPinDialog(onSet = { prefs.setPin(it); vm.unlocked = true }, onDismiss = { setPin = false })
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = bottomBar) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(s.tabSettings, style = MaterialTheme.typography.headlineSmall)

            // Google backup (next version)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ext.header)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Brand.GoldLight)
                    Spacer(Modifier.width(10.dp))
                    Text(s.backupTitle, color = Brand.Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(
                        s.comingSoon, color = Brand.EmeraldDeep, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brand.GoldLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(s.backupBody, color = Brand.Mint, fontSize = 13.sp)
            }

            SectionLabel(s.sectionData, Modifier.padding(start = 4.dp, top = 6.dp))
            Group {
                NavRow(s.exportExcel, iconRes = R.drawable.ic_pdf) {
                    scope.launch {
                        val txns = vm.allTxnsOnce()
                        if (txns.isEmpty()) {
                            Toast.makeText(ctx, s.nothingToExport, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val ks = vm.khatasOnce()
                        val file = withContext(Dispatchers.IO) { Share.exportCsv(ctx, ks, txns) }
                        Share.shareFile(ctx, file, "text/csv", s)
                    }
                }
            }

            SectionLabel(s.sectionApp, Modifier.padding(start = 4.dp, top = 6.dp))
            Group {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.language, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Segmented(
                        options = listOf("Roman Urdu", "English"),
                        selected = if (prefs.romanUrdu) 0 else 1,
                        onSelect = { prefs.updateRomanUrdu(it == 0) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider(color = ext.divider)
                SwitchRow(s.darkMode, prefs.dark) { prefs.updateDark(it) }
                HorizontalDivider(color = ext.divider)
                SwitchRow(s.pinLock, prefs.pinHash.isNotEmpty()) { on ->
                    if (on) setPin = true else prefs.setPin(null)
                }
                HorizontalDivider(color = ext.divider)
                ValueRow(s.currency, prefs.currency) { editCurrency = true }
                HorizontalDivider(color = ext.divider)
                ValueRow(s.businessName, prefs.business.ifBlank { s.notSet }) { editBusiness = true }
            }

            SectionLabel(s.sectionAbout, Modifier.padding(start = 4.dp, top = 6.dp))
            Group {
                ValueRow(s.version, "0.1.0 (test)", null)
                HorizontalDivider(color = ext.divider)
                ValueRow(s.developer, "Syed Hunain", null)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = LocalExt.current.muted)
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LocalExt.current.muted)
        }
    }
}

@Composable
private fun NavRow(label: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LocalExt.current.muted)
    }
}

@Composable
private fun SetPinDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStr.current
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(s.setPin) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin, onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4); error = false },
                    label = { Text(s.newPin) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = pin2, onValueChange = { v -> pin2 = v.filter { it.isDigit() }.take(4); error = false },
                    label = { Text(s.confirmPin) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                )
                if (error) Text(s.pinMismatch, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length == 4 && pin == pin2) {
                    onSet(pin); onDismiss()
                } else error = true
            }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
