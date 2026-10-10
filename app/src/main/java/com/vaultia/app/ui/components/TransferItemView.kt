package com.vaultia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.core.theme.StatusSuccess
import com.vaultia.app.core.theme.StatusWarning
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
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
    ClayCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Header Row: File Icon + Title + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transfer Type Pill Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (task.type == TransferType.UPLOAD)
                                VioletDuskRose.copy(alpha = 0.22f)
                            else
                                Color(0xFF38BDF8).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.type == TransferType.UPLOAD) Icons.Default.UploadFile else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (task.type == TransferType.UPLOAD) VioletDuskPeach else Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // File Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.filename,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (task.type == TransferType.UPLOAD) "Uploading to Vault" else "Downloading to Device",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Control Action Buttons (Clean fixed-size Box with proper spacing - NO overlap!)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.status == TransferStatus.TRANSFERRING) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StatusWarning.copy(alpha = 0.2f))
                                .clickable { onPause(task.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = StatusWarning, modifier = Modifier.size(16.dp))
                        }
                    } else if (task.status == TransferStatus.PAUSED || task.status == TransferStatus.FAILED) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.2f))
                                .clickable { onResume(task.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (task.status == TransferStatus.FAILED) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = StatusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (task.status.isActive || task.status == TransferStatus.FAILED || task.status == TransferStatus.PAUSED) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.2f))
                                .clickable { onCancel(task.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Metrics Row: Progress % + Bytes + Chunk Badge (Horizontally arranged)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${(task.progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VioletDuskPeach
                    )
                    Text(
                        text = "${VaultFile.formatBytes(task.transferredBytes)} / ${VaultFile.formatBytes(task.totalSize)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Chunk Count Badge (Never wraps vertically!)
                if (task.totalChunks > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VioletDuskPlum.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Chunk ${task.currentChunk} / ${task.totalChunks}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VioletDuskPeach
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shimmer Gradient Progress Bar
            ClayProgressBar(progress = task.progress, height = 7.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Status & Worker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (task.status) {
                    TransferStatus.TRANSFERRING -> "${task.formattedSpeed} • ETA ${task.formattedEta}"
                    TransferStatus.COMPLETED -> "Completed"
                    TransferStatus.PAUSED -> "Paused"
                    TransferStatus.FAILED -> "Failed: ${task.errorMessage ?: "Network timeout"}"
                    TransferStatus.CANCELLED -> "Cancelled"
                    TransferStatus.RECONSTRUCTING -> "Reconstructing file..."
                    TransferStatus.VERIFYING -> "Verifying checksum..."
                    else -> task.status.name
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (task.status) {
                        TransferStatus.COMPLETED -> StatusSuccess
                        TransferStatus.FAILED -> StatusError
                        TransferStatus.PAUSED -> StatusWarning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (task.activeWorkerName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(StatusSuccess, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Worker: ${task.activeWorkerName}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
