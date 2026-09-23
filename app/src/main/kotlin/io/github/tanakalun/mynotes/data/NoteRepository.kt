package io.github.tanakalun.mynotes.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.tanakalun.mynotes.utils.ImageUtils

object NoteRepository {

    private lateinit var db: NoteDatabase
    private var appContext: Context? = null

    var notesVersion by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        db = NoteDatabase(context)
        appContext = context.applicationContext
    }

    fun getAll(): List<Note> = db.getAll()

    fun getById(id: String): Note? = db.getById(id)

    fun search(query: String): List<Note> {
        if (query.isBlank()) return getAll()
        val q = query.lowercase()
        return getAll().filter {
            it.title.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.checklist.any { item -> item.text.lowercase().contains(q) }
        }
    }

    fun upsert(note: Note) {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        db.insertOrUpdate(updated)
        notesVersion++
    }

    fun togglePin(id: String) {
        val note = getById(id) ?: return
        db.insertOrUpdate(note.copy(pinned = !note.pinned))
        notesVersion++
    }

    fun delete(id: String) {
        val note = getById(id)
        note?.let { existingNote ->
            appContext?.let { ctx ->
                ImageUtils.deleteImages(ctx, existingNote.images)
                IMAGE_PATH_REGEX.findAll(existingNote.content).forEach { match ->
                    ImageUtils.deleteImage(ctx, match.groupValues[1])
                }
            }
        }
        db.delete(id)
        notesVersion++
    }

    fun exportData(context: Context): ByteArray = BackupManager.exportData(context)

    fun previewNoteCount(context: Context, uri: Uri): Int =
        BackupManager.previewNoteCount(context, uri)

    fun importData(context: Context, uri: Uri, overwriteSettings: Boolean): Int {
        val count = BackupManager.importData(context, uri, overwriteSettings)
        notesVersion++
        return count
    }

    private val IMAGE_PATH_REGEX = Regex("""!\[.*?\]\((.*?)\)""")
}
