package com.syedhunain.hisaabi.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.syedhunain.hisaabi.AppViewModel
import com.syedhunain.hisaabi.util.Ads
import com.syedhunain.hisaabi.util.GoogleDrive
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Returns a function that gets a Google access token for the Drive app folder.
 * The first time it shows Google's account picker / consent screen.
 * [onToken] receives the token, or null if the user cancelled or it failed.
 */
@Composable
fun rememberGoogleToken(onToken: (String?) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onToken)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val token = try {
                Identity.getAuthorizationClient(ctx).getAuthorizationResultFromIntent(res.data).accessToken
            } catch (e: Exception) {
                showGoogleError(ctx, e)
                null
            }
            callback(token)
        } else {
            callback(null)
        }
    }
    return {
        scope.launch {
            try {
                val result = GoogleDrive.authorize(ctx)
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    Ads.suppressNextReturn()
                    launcher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    callback(result.accessToken)
                }
            } catch (e: Exception) {
                showGoogleError(ctx, e)
                callback(null)
            }
        }
    }
}

/**
 * Shows Google's error code so sign-in problems can be diagnosed:
 * 10 = app not registered (package name / SHA-1 missing in Google Cloud),
 * 7 = no internet, 12501 / 16 = cancelled.
 */
private fun showGoogleError(ctx: Context, e: Exception) {
    val code = (e as? ApiException)?.statusCode
    val hint = when (code) {
        10 -> "App ki SHA-1 Google Cloud mein registered nahi (DEVELOPER_ERROR)"
        7 -> "Internet nahi mila"
        12501, 16 -> "Login cancel ho gaya"
        else -> e.javaClass.simpleName
    }
    Toast.makeText(ctx, "Google error ${code ?: "-"}: $hint", Toast.LENGTH_LONG).show()
}

fun toast(ctx: Context, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()

/** Shown when a Drive backup is found: asks before replacing the phone's data. */
@Composable
fun RestoreDialog(vm: AppViewModel) {
    val info = vm.restoreOffer ?: return
    val s = LocalStr.current
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = { vm.dismissRestore() },
        title = { Text(s.backupFound) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.backupFoundBody(Fmt.dateTime(info.modifiedAt)))
                Text(s.restoreWarning, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.confirmRestore { r ->
                    toast(ctx, if (r == AppViewModel.BackupResult.OK) s.restoreDone else s.cloudError)
                }
            }) { Text(s.restoreNow) }
        },
        dismissButton = { TextButton(onClick = { vm.dismissRestore() }) { Text(s.notNow) } },
    )
}
