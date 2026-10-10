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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.StatusError
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.VaultFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileCardItem(
    file: VaultFile,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onMoveToFolder: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val icon: ImageVector
    val iconTint: Color
    val iconBg: Color

    when (file.mediaType) {
        MediaType.VIDEOS -> {
            icon = Icons.Default.Movie
            iconTint = Color(0xFFA855F7)
            iconBg = Color(0xFFA855F7).copy(alpha = 0.15f)
        }
        MediaType.IMAGES -> {
            icon = Icons.Default.Image
            iconTint = Color(0xFFEC4899)
            iconBg = Color(0xFFEC4899).copy(alpha = 0.15f)
        }
        MediaType.AUDIO -> {
            icon = Icons.Default.Audiotrack
            iconTint = Color(0xFF06B6D4)
            iconBg = Color(0xFF06B6D4).copy(alpha = 0.15f)
        }
        MediaType.DOCUMENTS, MediaType.ALL -> {
            icon = Icons.Default.Description
            iconTint = Color(0xFFF97316)
            iconBg = Color(0xFFF97316).copy(alpha = 0.15f)
        }
        else -> {
            icon = Icons.Default.Description
            iconTint = Color(0xFF94A3B8)
            iconBg = Color(0xFF94A3B8).copy(alpha = 0.15f)
        }
    }

        ClayCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val iconType = when (file.mediaType) {
                    MediaType.VIDEOS -> VaultiaIconType.VIDEO
                    MediaType.AUDIO -> VaultiaIconType.AUDIO
                    MediaType.IMAGES -> VaultiaIconType.PHOTO
                    MediaType.DOCUMENTS -> VaultiaIconType.DOCUMENT
                    else -> VaultiaIconType.DOCUMENT
                }
                Vaultia3DIcon(type = iconType, size = 42.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${file.formattedSize} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(file.createdAt))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (file.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (file.isFavorite) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDownload()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = StatusError) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
