package com.vaultia.app.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vaultia.app.core.Constants
import com.vaultia.app.core.VaultResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.buffer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class TelegramApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    var customBaseUrl: String? = null

    val baseUrl: String
        get() = customBaseUrl?.trim()?.removeSuffix("/")?.takeIf { it.isNotBlank() } ?: Constants.TELEGRAM_API_BASE_URL

    suspend fun getMe(token: String): VaultResult<UserDto> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/bot$token/getMe"
        executeGet(url, object : TypeToken<TelegramApiResponse<UserDto>>() {}.type)
    }

    suspend fun getChat(token: String, chatId: String): VaultResult<ChatDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "$baseUrl/bot$token/getChat?chat_id=$formattedChatId"
        executeGet(url, object : TypeToken<TelegramApiResponse<ChatDto>>() {}.type)
    }

    suspend fun getChatMember(token: String, chatId: String, userId: Long): VaultResult<ChatMemberDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "$baseUrl/bot$token/getChatMember?chat_id=$formattedChatId&user_id=$userId"
        executeGet(url, object : TypeToken<TelegramApiResponse<ChatMemberDto>>() {}.type)
    }

    suspend fun sendDocumentChunk(
        token: String,
        chatId: String,
        chunkFile: File,
        fileName: String,
        caption: String,
        onProgress: ((uploadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): VaultResult<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val formattedChatId = formatChatId(chatId)
            val url = "$baseUrl/bot$token/sendDocument"

            val fileRequestBody = CountingRequestBody(
                requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), chunkFile),
                onProgress = onProgress
            )

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", formattedChatId)
                .addFormDataPart("document", fileName, fileRequestBody)
                .addFormDataPart("caption", caption)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(multipartBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val type = object : TypeToken<TelegramApiResponse<MessageDto>>() {}.type
                val apiResponse: TelegramApiResponse<MessageDto> = gson.fromJson(bodyStr, type)

                if (apiResponse.ok && apiResponse.result != null) {
                    VaultResult.Success(apiResponse.result)
                } else {
                    val code = apiResponse.errorCode ?: response.code
                    val desc = apiResponse.description ?: "Telegram API upload error"
                    VaultResult.Error(desc, code = code)
                }
            }
        } catch (e: Exception) {
            VaultResult.Error("Network error during chunk upload: ${e.message}", e)
        }
    }

    suspend fun getFile(token: String, fileId: String): VaultResult<FileDto> = withContext(Dispatchers.IO) {
        val encodedFileId = java.net.URLEncoder.encode(fileId, "UTF-8")
        val url = "$baseUrl/bot$token/getFile?file_id=$encodedFileId"
        executeGet(url, object : TypeToken<TelegramApiResponse<FileDto>>() {}.type)
    }

    suspend fun downloadFileChunk(
        token: String,
        filePath: String,
        outputStream: OutputStream,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): VaultResult<Long> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/file/bot$token/$filePath"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext VaultResult.Error("Download failed with HTTP ${response.code}", code = response.code)
                }
                val body = response.body ?: return@withContext VaultResult.Error("Empty download response body")
                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    onProgress?.invoke(totalRead, contentLength)
                }
                outputStream.flush()
                VaultResult.Success(totalRead)
            }
        } catch (e: Exception) {
            VaultResult.Error("Download error: ${e.message}", e)
        }
    }

    suspend fun deleteMessage(token: String, chatId: String, messageId: Long): VaultResult<Boolean> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "$baseUrl/bot$token/deleteMessage?chat_id=$formattedChatId&message_id=$messageId"
        executeGet(url, object : TypeToken<TelegramApiResponse<Boolean>>() {}.type)
    }

    suspend fun getUpdates(token: String, offset: Long? = null, limit: Int = 100, timeout: Int = 20): VaultResult<List<UpdateDto>> = withContext(Dispatchers.IO) {
        val offsetParam = if (offset != null) "&offset=$offset" else ""
        val url = "$baseUrl/bot$token/getUpdates?timeout=$timeout&limit=$limit$offsetParam"
        executeGet(url, object : TypeToken<TelegramApiResponse<List<UpdateDto>>>() {}.type)
    }

    suspend fun copyMessage(
        token: String,
        chatId: String,
        fromChatId: Long,
        messageId: Long,
        caption: String? = null
    ): VaultResult<MessageIdDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val captionParam = if (caption != null) "&caption=" + java.net.URLEncoder.encode(caption, "UTF-8") else ""
        val url = "$baseUrl/bot$token/copyMessage?chat_id=$formattedChatId&from_chat_id=$fromChatId&message_id=$messageId$captionParam"
        executeGet(url, object : TypeToken<TelegramApiResponse<MessageIdDto>>() {}.type)
    }

    suspend fun sendMessage(token: String, chatId: Long, text: String): VaultResult<MessageDto> = withContext(Dispatchers.IO) {
        val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "$baseUrl/bot$token/sendMessage?chat_id=$chatId&text=$encodedText"
        executeGet(url, object : TypeToken<TelegramApiResponse<MessageDto>>() {}.type)
    }

    suspend fun sendMessage(token: String, chatId: String, text: String): VaultResult<MessageDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "$baseUrl/bot$token/sendMessage?chat_id=$formattedChatId&text=$encodedText"
        executeGet(url, object : TypeToken<TelegramApiResponse<MessageDto>>() {}.type)
    }

    suspend fun forwardMessage(
        token: String,
        chatId: String,
        fromChatId: String,
        messageId: Long
    ): VaultResult<MessageDto> = withContext(Dispatchers.IO) {
        val targetChatId = formatChatId(chatId)
        val sourceChatId = formatChatId(fromChatId)
        val url = "$baseUrl/bot$token/forwardMessage?chat_id=$targetChatId&from_chat_id=$sourceChatId&message_id=$messageId"
        executeGet(url, object : TypeToken<TelegramApiResponse<MessageDto>>() {}.type)
    }

    suspend fun pinChatMessage(
        token: String,
        chatId: String,
        messageId: Long,
        disableNotification: Boolean = true
    ): VaultResult<Boolean> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "$baseUrl/bot$token/pinChatMessage?chat_id=$formattedChatId&message_id=$messageId&disable_notification=$disableNotification"
        executeGet(url, object : TypeToken<TelegramApiResponse<Boolean>>() {}.type)
    }

    suspend fun unpinChatMessage(
        token: String,
        chatId: String,
        messageId: Long
    ): VaultResult<Boolean> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "$baseUrl/bot$token/unpinChatMessage?chat_id=$formattedChatId&message_id=$messageId"
        executeGet(url, object : TypeToken<TelegramApiResponse<Boolean>>() {}.type)
    }

    suspend fun getUserProfilePhotos(token: String, userId: Long): VaultResult<UserProfilePhotosDto> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/bot$token/getUserProfilePhotos?user_id=$userId&limit=1"
        executeGet(url, object : TypeToken<TelegramApiResponse<UserProfilePhotosDto>>() {}.type)
    }

    private fun <T> executeGet(url: String, type: java.lang.reflect.Type): VaultResult<T> {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val apiResponse: TelegramApiResponse<T> = gson.fromJson(bodyStr, type)

                if (apiResponse.ok && apiResponse.result != null) {
                    VaultResult.Success(apiResponse.result)
                } else {
                    val code = apiResponse.errorCode ?: response.code
                    val desc = apiResponse.description ?: "Telegram API request failed"
                    android.util.Log.e("TelegramApiService", "Telegram API error for url $url: code=$code, desc=$desc")
                    VaultResult.Error(desc, code = code)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramApiService", "Network failure for url $url: ${e.message}", e)
            VaultResult.Error("Network failure: ${e.message}", e)
        }
    }

    private fun formatChatId(chatId: String): String {
        val trimmed = chatId.trim()
        return if (trimmed.startsWith("-100") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            trimmed
        } else if (trimmed.toLongOrNull() != null) {
            "-100$trimmed"
        } else {
            trimmed
        }
    }

    private class CountingRequestBody(
        private val requestBody: RequestBody,
        private val onProgress: ((uploadedBytes: Long, totalBytes: Long) -> Unit)?
    ) : RequestBody() {

        override fun contentType() = requestBody.contentType()

        override fun contentLength() = requestBody.contentLength()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = contentLength()
            var uploadedBytes = 0L

            val countingSink = object : okio.ForwardingSink(sink) {
                override fun write(source: okio.Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    uploadedBytes += byteCount
                    onProgress?.invoke(uploadedBytes, totalBytes)
                }
            }

            val bufferedSink = countingSink.buffer()
            requestBody.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }
}
