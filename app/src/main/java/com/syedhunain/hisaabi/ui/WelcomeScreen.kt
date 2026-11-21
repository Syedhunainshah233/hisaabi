package com.syedhunain.hisaabi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syedhunain.hisaabi.AppViewModel

/** First screen on a fresh install: sign in with Google (to restore a backup) or start without it. */
@Composable
fun WelcomeScreen(vm: AppViewModel) {
    val s = LocalStr.current
    val ctx = LocalContext.current
    val signIn = rememberGoogleToken { token ->
        if (token == null) {
            toast(ctx, s.signInFailed)
        } else {
            vm.onSignedIn(token, lookForBackup = true) { r ->
                if (r == AppViewModel.BackupResult.OK) vm.prefs.markWelcomeSeen()
                else toast(ctx, s.cloudError)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brand.EmeraldDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Logo(size = 112.dp, corner = 28.dp)
            Text("Hisaabi", color = Brand.Ivory, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(s.tagline, color = Brand.Mint, fontSize = 16.sp, textAlign = TextAlign.Center)
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.Ivory.copy(alpha = 0.08f))
                    .padding(14.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Brand.GoldLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(s.welcomePrivacy, color = Brand.Mint, fontSize = 13.sp, lineHeight = 19.sp)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.Ivory)
                    .bounceClick(enabled = !vm.cloudBusy) { signIn() }
                    .height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (vm.cloudBusy) {
                    CircularProgressIndicator(color = Brand.Emerald, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Brand.Emerald)
                }
                Spacer(Modifier.width(12.dp))
                Text(s.signInGoogle, color = Brand.EmeraldDeep, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = { vm.prefs.markWelcomeSeen() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(s.startWithoutLogin, color = Brand.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
