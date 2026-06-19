package com.prawalsharma.inkleaf.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

object ImageAttachmentStore {
    private const val DIR_NAME = "note_images"

    fun copyIntoAppStorage(context: Context, source: Uri): String? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(source)
        val extension = mimeType
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"

        val dir = File(context.filesDir, DIR_NAME).also { it.mkdirs() }
        val file = File(dir, "image_${UUID.randomUUID()}.$extension")

        return try {
            resolver.openInputStream(source)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun deleteIfOwned(pathOrUri: String) {
        val file = File(pathOrUri)
        if (file.isAbsolute && file.parentFile?.name == DIR_NAME) {
            file.delete()
        }
    }

    fun modelFor(pathOrUri: String): Any {
        val file = File(pathOrUri)
        return if (file.isAbsolute) file else pathOrUri
    }
}
