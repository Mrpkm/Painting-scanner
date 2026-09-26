package com.example.photomosaicscanner.session

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.photomosaicscanner.model.ScanSession
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Reads and writes session state to a JSON sidecar file inside an
 * app-specific external storage folder:
 *
 *   Android/data/com.example.photomosaicscanner/files/Pictures/PhotoMosaicScanner/<session>/
 *
 * This is a deliberate trade-off, not an oversight: writing arbitrary files
 * (the session.json alongside the photos) directly into the public
 * Pictures/ directory requires either the legacy WRITE_EXTERNAL_STORAGE
 * permission (deprecated, and unreliable on modern Android) or the Storage
 * Access Framework (adds real UI complexity for Stage 1). App-specific
 * storage needs no runtime permission and behaves identically across every
 * supported Android version.
 *
 * Trade-off: files here are not visible in the Gallery app and sit under a
 * package-specific path. You can still get them onto your Windows PC by:
 *  - connecting via USB and browsing to the path above with a file
 *    manager / Android File Transfer, or
 *  - using `adb pull` for the session folder.
 * If you'd rather have photos land in the public Pictures folder directly,
 * that's a reasonable Stage 2 addition (MediaStore + a Storage Access
 * Framework folder picker) -- flagged here rather than silently promised.
 */
object SessionRepository {

    private const val PREFS_NAME = "photomosaic_prefs"
    private const val KEY_ACTIVE_SESSION_DIR = "active_session_dir"

    fun rootDir(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val root = File(base, "PhotoMosaicScanner")
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun createSessionFolder(context: Context): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val root = rootDir(context)
        var index = 1
        var folder: File
        do {
            folder = File(root, "Painting_${stamp}_%03d".format(index))
            index += 1
        } while (folder.exists())
        folder.mkdirs()
        return folder
    }

    fun sessionFile(sessionDir: File): File = File(sessionDir, "session.json")

    fun save(context: Context, session: ScanSession) {
        val dir = File(session.sessionFolderPath)
        if (!dir.exists()) dir.mkdirs()
        sessionFile(dir).writeText(session.toJson().toString(2))
        setActiveSessionDir(context, dir)
    }

    fun loadFrom(sessionDir: File): ScanSession? {
        val file = sessionFile(sessionDir)
        if (!file.exists()) return null
        return try {
            ScanSession.fromJson(JSONObject(file.readText()))
        } catch (e: Exception) {
            null
        }
    }

    fun loadActiveSession(context: Context): ScanSession? {
        val path = activeSessionDir(context) ?: return null
        return loadFrom(File(path))
    }

    fun newSessionId(): String = UUID.randomUUID().toString()

    fun currentDeviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    fun setActiveSessionDir(context: Context, dir: File) {
        prefs(context).edit().putString(KEY_ACTIVE_SESSION_DIR, dir.absolutePath).apply()
    }

    fun clearActiveSession(context: Context) {
        prefs(context).edit().remove(KEY_ACTIVE_SESSION_DIR).apply()
    }

    private fun activeSessionDir(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE_SESSION_DIR, null)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
