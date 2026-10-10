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
    @SerializedName("description") val description: String? = null,
    @SerializedName("pinned_message") val pinnedMessage: MessageDto? = null
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
    @SerializedName("from") val from: UserDto? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("document") val document: DocumentDto? = null,
    @SerializedName("video") val video: VideoDto? = null,
    @SerializedName("animation") val animation: AnimationDto? = null,
    @SerializedName("audio") val audio: AudioDto? = null,
    @SerializedName("photo") val photo: List<PhotoSizeDto>? = null,
    @SerializedName("voice") val voice: VoiceDto? = null,
    @SerializedName("caption") val caption: String? = null
)

data class AnimationDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("thumb") val thumb: PhotoSizeDto? = null
)

data class DocumentDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("thumb") val thumb: PhotoSizeDto? = null
)

data class VideoDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("thumb") val thumb: PhotoSizeDto? = null
)

data class AudioDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("performer") val performer: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("file_name") val fileName: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null
)

data class PhotoSizeDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("file_size") val fileSize: Long? = null
)

data class VoiceDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null
)

data class FileDto(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Long? = null,
    @SerializedName("file_path") val filePath: String? = null
)

data class MessageIdDto(
    @SerializedName("message_id") val messageId: Long
)

data class UpdateDto(
    @SerializedName("update_id") val updateId: Long,
    @SerializedName("message") val message: MessageDto? = null,
    @SerializedName("channel_post") val channelPost: MessageDto? = null
)

data class UserProfilePhotosDto(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("photos") val photos: List<List<PhotoSizeDto>> = emptyList()
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
