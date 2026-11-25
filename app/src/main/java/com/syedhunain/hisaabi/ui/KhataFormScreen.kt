package com.syedhunain.hisaabi.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.util.Ads
import com.syedhunain.hisaabi.util.BannerAd

/** Opens the system contact picker on phone numbers; no contacts permission needed. */
private class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private fun readContact(ctx: Context, uri: Uri): Pair<String, String>? = runCatching {
    ctx.contentResolver.query(
        uri,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        ),
        null, null, null,
    )?.use { c ->
        if (c.moveToFirst()) (c.getString(0) ?: "") to (c.getString(1) ?: "") else null
    }
}.getOrNull()

@Composable
fun KhataFormScreen(
    vm: AppViewModel,
    khataId: Long?,
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
) {
    val s = LocalStr.current
    val ext = LocalExt.current
    val ctx = LocalContext.current
    val cur = vm.prefs.currency
    val items by vm.khatas.collectAsState()
    val existing = khataId?.let { id -> items.firstOrNull { it.khata.id == id }?.khata }

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(Khata.TYPE_CUSTOMER) }
    var address by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var openingText by rememberSaveable { mutableStateOf("") }
    var openingGave by rememberSaveable { mutableStateOf(true) }
    var loaded by rememberSaveable { mutableStateOf(khataId == null) }
    var nameError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!loaded && existing != null) {
            name = existing.name; phone = existing.phone; type = existing.type
            address = existing.address; note = existing.note
            loaded = true
        }
    }

    val picker = rememberLauncherForActivityResult(PickPhone()) { uri ->
        if (uri != null) {
            readContact(ctx, uri)?.let { (n, p) ->
                if (name.isBlank()) name = n
                phone = p
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back) }
            Text(if (khataId == null) s.createKhata else s.editKhata, style = MaterialTheme.typography.titleLarge)
        }

        BannerAd(vm.prefs, Modifier.padding(top = 8.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50); nameError = false },
                label = { Text(s.name) },
                isError = nameError,
                supportingText = if (nameError) ({ Text(s.nameError) }) else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { v -> phone = v.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(20) },
                label = { Text(s.mobile) },
                placeholder = { Text("03XX XXXXXXX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    IconButton(onClick = { Ads.suppressNextReturn(); runCatching { picker.launch(Unit) } }) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = s.contacts,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(s.khataType)
            val types = listOf(Khata.TYPE_CUSTOMER, Khata.TYPE_SUPPLIER, Khata.TYPE_FRIEND, Khata.TYPE_PERSONAL)
            types.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { t ->
                        Choice(s.typeLabel(t), type == t, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f)) { type = t }
                    }
                }
            }

            if (khataId == null) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = openingText,
                        onValueChange = { v -> openingText = v.filter { it.isDigit() }.take(11).trimStart('0') },
                        label = { Text(s.openingOptional) },
                        prefix = { Text("$cur ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice(s.iWillGet, openingGave, ext.green, ext.greenSoft, Modifier.weight(1f)) { openingGave = true }
                        Choice(s.iWillGive, !openingGave, ext.red, ext.redSoft, Modifier.weight(1f)) { openingGave = false }
                    }
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it.take(80) },
                label = { Text(s.address) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(120) },
                label = { Text(s.noteOptional) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brand.Emerald)
                .bounceClick {
                    if (name.isBlank()) {
                        nameError = true
                    } else if (!saving) {
                        saving = true
                        vm.saveKhata(
                            id = khataId, name = name, phone = phone, type = type,
                            address = address, note = note,
                            opening = openingText.toLongOrNull() ?: 0L, openingGave = openingGave,
                        ) { id -> onSaved(id, khataId == null) }
                    }
                }
                .height(56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (khataId == null) Icons.Filled.Add else Icons.Filled.Check, contentDescription = null, tint = Brand.GoldLight)
            Spacer(Modifier.width(10.dp))
            Text(if (khataId == null) s.createBtn else s.save, color = Brand.Ivory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, color: Color, soft: Color, modifier: Modifier, onClick: () -> Unit) {
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
            textAlign = TextAlign.Center,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
        )
    }
}
