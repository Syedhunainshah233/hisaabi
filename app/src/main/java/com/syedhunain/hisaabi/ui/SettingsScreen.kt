package com.syedhunain.hisaabi.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import com.syedhunain.hisaabi.BuildConfig
import com.syedhunain.hisaabi.util.Ads
import com.syedhunain.hisaabi.util.BannerAd
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
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

            GoogleBackupCard(vm)

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

            if (Ads.ready) {
                Group { AdsOffRow(vm) }
            }

            BannerAd(prefs, Modifier.padding(top = 4.dp))

            if (SHOW_SUPPORT) {
                SupportCard()
            }

            SectionLabel(s.sectionAbout, Modifier.padding(start = 4.dp, top = 6.dp))
            Group {
                ValueRow(s.version, BuildConfig.VERSION_NAME, null)
                HorizontalDivider(color = ext.divider)
                ValueRow(s.developer, DEVELOPER_NAME, null)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

const val DEVELOPER_NAME = "SyedTechdevelopers"

@Composable
private fun GoogleBackupCard(vm: AppViewModel) {
    val s = LocalStr.current
    val ctx = LocalContext.current
    val prefs = vm.prefs
    val signedIn = prefs.googleEmail.isNotEmpty()
    var action by remember { mutableStateOf("") }

    val getToken = rememberGoogleToken { token ->
        if (token == null) {
            toast(ctx, s.signInFailed)
            return@rememberGoogleToken
        }
        when (action) {
            "signin" -> vm.onSignedIn(token, lookForBackup = true) { r ->
                if (r != AppViewModel.BackupResult.OK) toast(ctx, s.cloudError)
            }
            "backup" -> vm.backupNow(token) { r ->
                if (r == AppViewModel.BackupResult.OK) {
                    toast(ctx, s.backupDone)
                    Ads.onBreak(ctx.findActivity(), prefs, "backup", every = 1)
                } else toast(ctx, s.cloudError)
            }
            "restore" -> vm.findBackupToRestore(token) { r ->
                when (r) {
                    AppViewModel.BackupResult.NO_BACKUP -> toast(ctx, s.noBackup)
                    AppViewModel.BackupResult.ERROR -> toast(ctx, s.cloudError)
                    else -> Unit
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalExt.current.header)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Brand.GoldLight)
            Spacer(Modifier.width(10.dp))
            Text(s.backupTitle, color = Brand.Ivory, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (vm.cloudBusy) {
                CircularProgressIndicator(color = Brand.GoldLight, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            }
        }
        if (!signedIn) {
            Text(s.backupSignedOutBody, color = Brand.Mint, fontSize = 13.sp)
            CardButton(s.signInGoogle, enabled = !vm.cloudBusy) { action = "signin"; getToken() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    prefs.googleName.ifBlank { prefs.googleEmail }, size = 40.dp,
                    bg = Brand.GoldLight, fg = Brand.EmeraldDeep,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        prefs.googleEmail, color = Brand.Ivory, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(s.connected + " ✓", color = Brand.Mint, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Brand.Ivory.copy(alpha = 0.2f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.lastBackup, color = Brand.Mint, fontSize = 12.sp)
                    Text(
                        if (prefs.lastBackupAt > 0) Fmt.relative(prefs.lastBackupAt, s) else s.never,
                        color = Brand.Ivory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                CardButton(s.backupNow, enabled = !vm.cloudBusy, fill = false) { action = "backup"; getToken() }
            }
            Text(s.autoBackupNote, color = Brand.Mint, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = { action = "restore"; getToken() }, enabled = !vm.cloudBusy) {
                    Text(s.restoreRow, color = Brand.GoldLight, fontSize = 13.sp)
                }
                TextButton(onClick = { vm.signOut() }) {
                    Text(s.signOut, color = Brand.Ivory.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CardButton(text: String, enabled: Boolean, fill: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .background(Brand.Ivory)
            .bounceClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Brand.EmeraldDeep, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AdsOffRow(vm: AppViewModel) {
    val s = LocalStr.current
    val ctx = LocalContext.current
    val prefs = vm.prefs
    val off = prefs.adsOffUntil > System.currentTimeMillis()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !off) {
                val act = ctx.findActivity() ?: return@clickable
                Ads.showRewarded(
                    act,
                    onReward = {
                        prefs.turnAdsOffUntil(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
                        toast(ctx, s.adsOffDone)
                    },
                    onFailed = { toast(ctx, s.adNotReady) },
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.adsOffTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                if (off) s.adsOffUntil(Fmt.dateTime(prefs.adsOffUntil)) else s.adsOffBody,
                fontSize = 12.sp, color = LocalExt.current.muted,
            )
        }
    }
}

// Turn off before a Play Store release: Google Play does not allow
// in-app donations outside Play billing (except registered charities).
private const val SHOW_SUPPORT = true
private const val SUPPORT_BANK = "NayaPay"
private const val SUPPORT_IBAN = "PK48NAYA1234503265904350"

@Composable
private fun SupportCard() {
    val s = LocalStr.current
    val ext = LocalExt.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.goldSoft)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = ext.red, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.supportTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(s.supportBody, fontSize = 13.sp, color = ext.onGoldSoft)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(SUPPORT_BANK, fontSize = 12.sp, color = ext.muted)
                Text(
                    SUPPORT_IBAN,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(DEVELOPER_NAME, fontSize = 12.sp, color = ext.muted)
            }
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(SUPPORT_IBAN))
                Toast.makeText(ctx, s.copied, Toast.LENGTH_SHORT).show()
            }) { Text(s.copy, fontWeight = FontWeight.SemiBold) }
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
