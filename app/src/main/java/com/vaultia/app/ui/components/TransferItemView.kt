package com.vaultia.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.core.theme.StatusWarning
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.TransferTask
import com.vaultia.app.domain.model.TransferType
import com.vaultia.app.domain.model.VaultFile

@Composable
fun TransferItemView(
    task: TransferTask,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ClayCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (task.type == TransferType.UPLOAD) Icons.Default.UploadFile else Icons.Default.Download,
                    contentDescription = null,
                    tint = ClayPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.filename,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(task.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClayPrimary
                        )
                        Text(
                            text = "${VaultFile.formatBytes(task.transferredBytes)} / ${VaultFile.formatBytes(task.totalSize)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (task.totalChunks > 1) {
                            Text(
                                text = "Chunk ${task.currentChunk}/${task.totalChunks}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Control Buttons
                Row {
                    if (task.status == TransferStatus.TRANSFERRING) {
                        IconButton(onClick = { onPause(task.id) }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = StatusWarning)
                        }
                    } else if (task.status == TransferStatus.PAUSED) {
                        IconButton(onClick = { onResume(task.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = StatusSuccess)
                        }
                    }

                    if (task.status.isActive) {
                        IconButton(onClick = { onCancel(task.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ClayProgressBar(progress = task.progress)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (task.status) {
                    TransferStatus.TRANSFERRING -> "${task.formattedSpeed} • ETA ${task.formattedEta}"
                    TransferStatus.COMPLETED -> "Completed"
                    TransferStatus.PAUSED -> "Paused"
                    TransferStatus.FAILED -> "Failed: ${task.errorMessage ?: "Unknown error"}"
                    TransferStatus.CANCELLED -> "Cancelled"
                    TransferStatus.RECONSTRUCTING -> "Reconstructing file..."
                    TransferStatus.VERIFYING -> "Verifying checksum..."
                    else -> task.status.name
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = when (task.status) {
                        TransferStatus.COMPLETED -> StatusSuccess
                        TransferStatus.FAILED -> StatusError
                        TransferStatus.PAUSED -> StatusWarning
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )

                if (task.activeWorkerName != null) {
                    Text(
                        text = "Worker: ${task.activeWorkerName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
