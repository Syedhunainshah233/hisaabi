package com.syedhunain.hisaabi.util

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.syedhunain.hisaabi.data.Khata
import com.syedhunain.hisaabi.data.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/** Info about the backup file kept in the user's hidden Drive app folder. */
data class BackupInfo(val id: String, val modifiedAt: Long, val size: Long)

/**
 * Google sign-in for backup, and the Drive REST calls.
 * Only the hidden app data folder is used (drive.appdata): Hisaabi cannot see other Drive files.
 */
object GoogleDrive {
    private const val FILE_NAME = "hisaabi_backup.json"
    private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    private val request: AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE_APPDATA), Scope("email"), Scope("profile")))
        .build()

    /** Returns either an access token, or a result that needs the user to choose/approve (hasResolution). */
    suspend fun authorize(context: Context): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(request).await()

    // ---------- HTTP helpers ----------

    private fun open(url: String, token: String, method: String = "GET"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun HttpURLConnection.readBody(): String {
        val code = responseCode
        val stream = if (code in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("HTTP $code: ${text.take(300)}")
        return text
    }

    // ---------- Drive calls ----------

    suspend fun userInfo(token: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val c = open("https://www.googleapis.com/oauth2/v3/userinfo", token)
        val json = JSONObject(c.readBody())
        json.optString("email") to json.optString("name")
    }

    suspend fun findBackup(token: String): BackupInfo? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q" +
            "&orderBy=modifiedTime%20desc&fields=files(id,modifiedTime,size)"
        val files = JSONObject(open(url, token).readBody()).optJSONArray("files") ?: JSONArray()
        if (files.length() == 0) return@withContext null
        val f = files.getJSONObject(0)
        BackupInfo(
            id = f.getString("id"),
            modifiedAt = runCatching { Instant.parse(f.getString("modifiedTime")).toEpochMilli() }.getOrDefault(0L),
            size = f.optString("size", "0").toLongOrNull() ?: 0L,
        )
    }

    /** Uploads a new backup file, then removes the older one(s). */
    suspend fun upload(token: String, content: String) = withContext(Dispatchers.IO) {
        val old = findAllIds(token)
        val boundary = "hisaabi" + System.currentTimeMillis()
        val meta = JSONObject()
            .put("name", FILE_NAME)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("mimeType", "application/json")
        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta.toString()).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(content).append("\r\n")
            append("--").append(boundary).append("--")
        }.toByteArray(Charsets.UTF_8)
        val c = open("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id", token, "POST")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        c.setFixedLengthStreamingMode(body.size)
        c.outputStream.use { it.write(body) }
        c.readBody()
        for (id in old) {
            runCatching { open("https://www.googleapis.com/drive/v3/files/$id", token, "DELETE").readBody() }
        }
    }

    private fun findAllIds(token: String): List<String> {
        val q = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$q&fields=files(id)"
        val files = JSONObject(open(url, token).readBody()).optJSONArray("files") ?: JSONArray()
        return (0 until files.length()).map { files.getJSONObject(it).getString("id") }
    }

    suspend fun download(token: String, id: String): String = withContext(Dispatchers.IO) {
        open("https://www.googleapis.com/drive/v3/files/$id?alt=media", token).readBody()
    }
}

/** Backup file format (JSON). */
object BackupJson {
    fun encode(khatas: List<Khata>, txns: List<Txn>): String {
        val ks = JSONArray()
        khatas.forEach { k ->
            ks.put(
                JSONObject()
                    .put("id", k.id).put("name", k.name).put("phone", k.phone).put("type", k.type)
                    .put("address", k.address).put("note", k.note)
                    .put("createdAt", k.createdAt).put("updatedAt", k.updatedAt)
            )
        }
        val ts = JSONArray()
        txns.forEach { t ->
            ts.put(
                JSONObject()
                    .put("id", t.id).put("khataId", t.khataId).put("amount", t.amount).put("gave", t.gave)
                    .put("note", t.note).put("date", t.date).put("isOpening", t.isOpening)
            )
        }
        return JSONObject()
            .put("app", "hisaabi").put("format", 1).put("createdAt", System.currentTimeMillis())
            .put("khatas", ks).put("txns", ts)
            .toString()
    }

    fun decode(text: String): Pair<List<Khata>, List<Txn>> {
        val root = JSONObject(text)
        require(root.optString("app") == "hisaabi") { "Not a Hisaabi backup" }
        val ks = root.optJSONArray("khatas") ?: JSONArray()
        val ts = root.optJSONArray("txns") ?: JSONArray()
        val khatas = (0 until ks.length()).map { i ->
            val o = ks.getJSONObject(i)
            Khata(
                id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone"),
                type = o.optString("type", Khata.TYPE_CUSTOMER), address = o.optString("address"),
                note = o.optString("note"), createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
            )
        }
        val txns = (0 until ts.length()).map { i ->
            val o = ts.getJSONObject(i)
            Txn(
                id = o.getLong("id"), khataId = o.getLong("khataId"), amount = o.getLong("amount"),
                gave = o.getBoolean("gave"), note = o.optString("note"), date = o.getLong("date"),
                isOpening = o.optBoolean("isOpening"),
            )
        }
        return khatas to txns
    }
}
