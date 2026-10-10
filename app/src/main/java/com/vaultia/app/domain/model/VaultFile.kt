package com.vaultia.app.domain.model

data class VaultFile(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val mediaType: MediaType,
    val totalChunks: Int,
    val chunkSize: Long,
    val checksum: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val remoteRootMessageId: Long? = null,
    val localUri: String? = null,
    val folderId: String? = null
) {
    val formattedSize: String
        get() = formatBytes(size)

    val extension: String
        get() = name.substringAfterLast('.', "")

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val unitIndex = digitGroups.coerceIn(0, units.size - 1)
            return String.format("%.1f %s", bytes / Math.pow(1024.0, unitIndex.toDouble()), units[unitIndex])
        }
    }
}
