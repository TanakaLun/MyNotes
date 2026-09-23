package io.github.tanakalun.mynotes.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class NoteDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION,
) {

    companion object {
        private const val DB_NAME = "notes.db"
        private const val DB_VERSION = 4

        private const val TABLE = "notes"
        private const val COL_ID = "id"
        private const val COL_TYPE = "type"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_CHECKLIST = "checklist"
        private const val COL_COLOR = "color_index"
        private const val COL_IMAGES = "images"
        private const val COL_PINNED = "pinned"
        private const val COL_CREATED = "created_at"
        private const val COL_UPDATED = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TYPE INTEGER NOT NULL DEFAULT 0,
                $COL_TITLE TEXT NOT NULL,
                $COL_CONTENT TEXT NOT NULL,
                $COL_CHECKLIST TEXT NOT NULL DEFAULT '[]',
                $COL_COLOR INTEGER NOT NULL DEFAULT 0,
                $COL_IMAGES TEXT NOT NULL DEFAULT '[]',
                $COL_PINNED INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED INTEGER NOT NULL,
                $COL_UPDATED INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_TYPE INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_CHECKLIST TEXT NOT NULL DEFAULT '[]'")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_IMAGES TEXT NOT NULL DEFAULT '[]'")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_PINNED INTEGER NOT NULL DEFAULT 0")
        }
    }

    fun insertOrUpdate(note: Note) {
        val encrypted = NoteEncryption.encrypt(note.title) to NoteEncryption.encrypt(note.content)
        val checklistJson = encodeChecklist(note.checklist)
        val imagesJson = encodeImages(note.images)
        val cv = ContentValues().apply {
            put(COL_ID, note.id)
            put(COL_TYPE, note.type.ordinal)
            put(COL_TITLE, encrypted.first)
            put(COL_CONTENT, encrypted.second)
            put(COL_CHECKLIST, checklistJson)
            put(COL_COLOR, note.colorIndex)
            put(COL_IMAGES, imagesJson)
            put(COL_PINNED, if (note.pinned) 1 else 0)
            put(COL_CREATED, note.createdAt)
            put(COL_UPDATED, note.updatedAt)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun delete(id: String) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id))
    }

    fun getAll(): List<Note> {
        val notes = mutableListOf<Note>()
        val cursor = readableDatabase.query(
            TABLE,
            null,
            null,
            null,
            null,
            null,
            "$COL_PINNED DESC, $COL_UPDATED DESC",
        )
        cursor.use {
            while (it.moveToNext()) {
                notes.add(readNote(it))
            }
        }
        return notes
    }

    fun getById(id: String): Note? {
        val cursor = readableDatabase.query(TABLE, null, "$COL_ID = ?", arrayOf(id), null, null, null)
        cursor.use {
            if (it.moveToFirst()) return readNote(it)
        }
        return null
    }

    private fun readNote(cursor: android.database.Cursor): Note {
        val typeOrdinal = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TYPE))
        return Note(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
            type = NoteType.entries.getOrElse(typeOrdinal) { NoteType.Note },
            title = try {
                NoteEncryption.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)))
            } catch (_: Exception) {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE))
            },
            content = try {
                NoteEncryption.decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)))
            } catch (_: Exception) {
                cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT))
            },
            checklist = try {
                decodeChecklist(cursor.getString(cursor.getColumnIndexOrThrow(COL_CHECKLIST)))
            } catch (_: Exception) {
                emptyList()
            },
            colorIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLOR)),
            images = try {
                decodeImages(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGES)))
            } catch (_: Exception) {
                emptyList()
            },
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED)),
            pinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PINNED)) != 0,
        )
    }

    private fun encodeChecklist(items: List<CheckItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("text", NoteEncryption.encrypt(item.text))
                put("checked", item.checked)
            })
        }
        return arr.toString()
    }

    private fun decodeChecklist(json: String): List<CheckItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            CheckItem(
                id = obj.getString("id"),
                text = try {
                    NoteEncryption.decrypt(obj.getString("text"))
                } catch (_: Exception) {
                    obj.optString("text", "")
                },
                checked = obj.optBoolean("checked", false),
            )
        }
    }

    private fun encodeImages(images: List<String>): String {
        val arr = JSONArray()
        images.forEach { path ->
            arr.put(path)
        }
        return arr.toString()
    }

    private fun decodeImages(json: String): List<String> {
        if (json.isBlank() || json == "[]") return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            arr.optString(i, null)
        }
    }
}
