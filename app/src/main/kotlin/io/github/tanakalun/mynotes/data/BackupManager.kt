package io.github.tanakalun.mynotes.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import io.github.tanakalun.mynotes.SettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupMetadata(
    val version: Int = 2,
    val appVersion: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class BackupCheckItem(
    val id: String,
    val text: String,
    val checked: Boolean,
)

@Serializable
data class BackupNote(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val checklist: List<BackupCheckItem>,
    val colorIndex: Int,
    val images: List<String>,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSettings(
    val colorMode: Int = 0,
    val keyColorIndex: Int = 0,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0,
    val enableBlur: Boolean = true,
    val blurStyle: Int = 0,
    val useFloatingNavbar: Boolean = false,
    val floatingNavbarStyle: Int = 0,
    val floatingNavbarPosition: Int = 0,
    val showSearchBar: Boolean = true,
    val enterCreatesItem: Boolean = false,
    val codeBlockWrap: Boolean = true,
)

@Serializable
data class BackupSettingsWrapper(
    val settings: BackupSettings,
)

object BackupManager {

    private const val META_FILE = "metadata.json"
    private const val NOTES_DIR = "notes/"
    private const val IMAGES_DIR = "images/"
    private const val SETTINGS_FILE = "settings.json"
    private const val IMAGE_PREFIX = "note_images/"

    private val json = Json { ignoreUnknownKeys = true }

    fun exportData(context: Context): ByteArray {
        val notes = NoteRepository.getAll()

        val tmpDir = File(context.cacheDir, "backup_export_${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        File(tmpDir, NOTES_DIR).mkdirs()
        File(tmpDir, IMAGES_DIR).mkdirs()

        try {
            val imageMap = mutableMapOf<String, String>()

            notes.forEachIndexed { index, note ->
                val noteImages = note.images + IMAGE_PATH_REGEX.findAll(note.content)
                    .map { it.groupValues[1] }.toList()

                noteImages.forEachIndexed { imgIdx, path ->
                    if (path in imageMap) return@forEachIndexed
                    val fileName = "img_${index}_$imgIdx${File(path).extension.let { if (it.isEmpty()) ".jpg" else it }}"
                    val file = File(path)
                    if (file.exists() && file.isFile) {
                        file.copyTo(File(tmpDir, "$IMAGES_DIR$fileName"), overwrite = true)
                        imageMap[path] = "$IMAGES_DIR$fileName"
                    }
                }

                val backupNote = BackupNote(
                    id = note.id,
                    type = note.type.name,
                    title = note.title,
                    content = note.content,
                    checklist = note.checklist.map {
                        BackupCheckItem(it.id, it.text, it.checked)
                    },
                    colorIndex = note.colorIndex,
                    images = note.images.map { imageMap[it] ?: it },
                    pinned = note.pinned,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                )
                File(tmpDir, "${NOTES_DIR}${note.id}.json").writeText(json.encodeToString(backupNote))
            }

            val backupSettings = BackupSettings(
                colorMode = SettingsStore.colorMode,
                keyColorIndex = SettingsStore.keyColorIndex,
                paletteStyle = SettingsStore.paletteStyle,
                colorSpec = SettingsStore.colorSpec,
                enableBlur = SettingsStore.enableBlur,
                blurStyle = SettingsStore.blurStyle,
                useFloatingNavbar = SettingsStore.useFloatingNavbar,
                floatingNavbarStyle = SettingsStore.floatingNavbarStyle,
                floatingNavbarPosition = SettingsStore.floatingNavbarPosition,
                showSearchBar = SettingsStore.showSearchBar,
                enterCreatesItem = SettingsStore.enterCreatesItem,
                codeBlockWrap = SettingsStore.codeBlockWrap,
            )
            File(tmpDir, SETTINGS_FILE).writeText(
                json.encodeToString(BackupSettingsWrapper(backupSettings)),
            )

            val metadata = BackupMetadata(createdAt = System.currentTimeMillis())
            File(tmpDir, META_FILE).writeText(json.encodeToString(metadata))

            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zip ->
                tmpDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val entryName = file.relativeTo(tmpDir).path
                    zip.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }

            return BackupCrypto.encrypt(baos.toByteArray())
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    fun previewNoteCount(context: Context, uri: Uri): Int {
        val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read file")
        val zipBytes = BackupCrypto.decrypt(encrypted)

        val tmpDir = File(context.cacheDir, "backup_preview_${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        try {
            ZipInputStream(zipBytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tmpDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val notesDir = File(tmpDir, "notes")
            return notesDir.listFiles()?.size ?: 0
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    fun importData(context: Context, uri: Uri, overwriteSettings: Boolean): Int {
        val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read file")
        val zipBytes = BackupCrypto.decrypt(encrypted)

        val tmpDir = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        try {
            ZipInputStream(zipBytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(tmpDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val db = NoteDatabase(context)

            val notesDir = File(tmpDir, "notes")
            val imageMap = mutableMapOf<String, String>()
            val notesToImport = mutableListOf<Note>()

            notesDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
                val backupNote = json.decodeFromString<BackupNote>(file.readText())

                val restoredImages = backupNote.images.map { zipPath ->
                    val imageFile = File(tmpDir, zipPath)
                    if (imageFile.exists()) {
                        val imagesDir = File(context.filesDir, "note_images")
                        imagesDir.mkdirs()
                        val targetName = "import_${System.currentTimeMillis()}_${imageFile.nameWithoutExtension}${imageFile.extension}"
                        val target = File(imagesDir, targetName)
                        imageFile.copyTo(target, overwrite = true)
                        target.absolutePath
                    } else {
                        zipPath
                    }
                }

                val dbNote = Note(
                    id = backupNote.id,
                    type = try { NoteType.valueOf(backupNote.type) } catch (_: Exception) { NoteType.Note },
                    title = backupNote.title,
                    content = backupNote.content,
                    checklist = backupNote.checklist.map { CheckItem(it.id, it.text, it.checked) },
                    colorIndex = backupNote.colorIndex,
                    images = restoredImages,
                    pinned = backupNote.pinned,
                    createdAt = backupNote.createdAt,
                    updatedAt = backupNote.updatedAt,
                )
                notesToImport.add(dbNote)
            }

            notesToImport.forEach { db.insertOrUpdate(it) }

            if (overwriteSettings) {
                val settingsFile = File(tmpDir, SETTINGS_FILE)
                if (settingsFile.exists()) {
                    val wrapper = json.decodeFromString<BackupSettingsWrapper>(settingsFile.readText())
                    val s = wrapper.settings
                    SettingsStore.updateColorMode(s.colorMode)
                    SettingsStore.updateKeyColorIndex(s.keyColorIndex)
                    SettingsStore.updatePaletteStyle(s.paletteStyle)
                    SettingsStore.updateColorSpec(s.colorSpec)
                    SettingsStore.updateEnableBlur(s.enableBlur)
                    SettingsStore.updateBlurStyle(s.blurStyle)
                    SettingsStore.updateUseFloatingNavbar(s.useFloatingNavbar)
                    SettingsStore.updateFloatingNavbarStyle(s.floatingNavbarStyle)
                    SettingsStore.updateFloatingNavbarPosition(s.floatingNavbarPosition)
                    SettingsStore.updateShowSearchBar(s.showSearchBar)
                    SettingsStore.updateEnterCreatesItem(s.enterCreatesItem)
                    SettingsStore.updateCodeBlockWrap(s.codeBlockWrap)
                }
            }

            return notesToImport.size
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private val IMAGE_PATH_REGEX = Regex("""!\[.*?\]\((.*?)\)""")
}
