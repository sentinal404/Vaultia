package com.vaultia.app.data.remote

import com.google.gson.annotations.SerializedName

data class TelegramApiResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: T? = null,
    @SerializedName("error_code") val errorCode: Int? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("parameters") val parameters: ResponseParameters? = null
)

data class ResponseParameters(
    @SerializedName("migrate_to_chat_id") val migrateToChatId: Long? = null,
    @SerializedName("retry_after") val retryAfter: Int? = null
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("can_join_groups") val canJoinGroups: Boolean? = null,
    @SerializedName("can_read_all_group_messages") val canReadAllGroupMessages: Boolean? = null
)

data class ChatDto(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String, // "channel", "supergroup", "group", "private"
    @SerializedName("title") val title: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("description") val description: String? = null
)

data class ChatMemberDto(
    @SerializedName("status") val status: String, // "creator", "administrator", "member", "restricted", "left", "kicked"
    @SerializedName("user") val user: UserDto,
    @SerializedName("can_post_messages") val canPostMessages: Boolean? = null,
    @SerializedName("can_edit_messages") val canEditMessages: Boolean? = null,
    @SerializedName("can_delete_messages") val canDeleteMessages: Boolean? = null
)

data class MessageDto(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("date") val date: Long,
    @SerializedName("chat") val chat: ChatDto,
    @SerializedName("document") val document: DocumentDto? = null,
    @SerializedName("caption") val caption: String? = null
)

data class DocumentDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null
)

data class FileDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("file_path") val filePath: String? = null
)

data class ChunkPayloadMetadata(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("fid") val fileId: String,
    @SerializedName("fn") val filename: String,
    @SerializedName("idx") val chunkIndex: Int,
    @SerializedName("tot") val totalChunks: Int,
    @SerializedName("cs") val chunkSize: Long,
    @SerializedName("sha") val checksum: String,
    @SerializedName("ts") val timestamp: Long = System.currentTimeMillis()
)
