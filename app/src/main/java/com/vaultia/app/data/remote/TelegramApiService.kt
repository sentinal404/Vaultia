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

    suspend fun getMe(token: String): VaultResult<UserDto> = withContext(Dispatchers.IO) {
        val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/getMe"
        executeGet(url, object : TypeToken<TelegramApiResponse<UserDto>>() {}.type)
    }

    suspend fun getChat(token: String, chatId: String): VaultResult<ChatDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/getChat?chat_id=$formattedChatId"
        executeGet(url, object : TypeToken<TelegramApiResponse<ChatDto>>() {}.type)
    }

    suspend fun getChatMember(token: String, chatId: String, userId: Long): VaultResult<ChatMemberDto> = withContext(Dispatchers.IO) {
        val formattedChatId = formatChatId(chatId)
        val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/getChatMember?chat_id=$formattedChatId&user_id=$userId"
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
            val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/sendDocument"

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
        val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/getFile?file_id=$fileId"
        executeGet(url, object : TypeToken<TelegramApiResponse<FileDto>>() {}.type)
    }

    suspend fun downloadFileChunk(
        token: String,
        filePath: String,
        outputStream: OutputStream,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null
    ): VaultResult<Long> = withContext(Dispatchers.IO) {
        try {
            val url = "${Constants.TELEGRAM_API_BASE_URL}/file/bot$token/$filePath"
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
        val url = "${Constants.TELEGRAM_API_BASE_URL}/bot$token/deleteMessage?chat_id=$formattedChatId&message_id=$messageId"
        executeGet(url, object : TypeToken<TelegramApiResponse<Boolean>>() {}.type)
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
                    VaultResult.Error(desc, code = code)
                }
            }
        } catch (e: Exception) {
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
