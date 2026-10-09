package com.vaultia.app.domain.model

enum class MediaType(val displayName: String) {
    ALL("All Files"),
    VIDEOS("Videos"),
    IMAGES("Images"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    ARCHIVES("Archives"),
    OTHER("Other");

    companion object {
        fun fromMimeType(mimeType: String?, filename: String): MediaType {
            val mime = mimeType?.lowercase() ?: ""
            val ext = filename.substringAfterLast('.', "").lowercase()

            return when {
                mime.startsWith("video/") || ext in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v") -> VIDEOS
                mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic") -> IMAGES
                mime.startsWith("audio/") || ext in listOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma") -> AUDIO
                mime.startsWith("text/") || mime.contains("pdf") || mime.contains("document") || mime.contains("sheet") ||
                        ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "md", "csv", "json") -> DOCUMENTS
                ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> ARCHIVES
                else -> OTHER
            }
        }
    }
}
