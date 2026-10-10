package com.vaultia.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.VioletDuskCard
import com.vaultia.app.core.theme.VioletDuskCream
import com.vaultia.app.core.theme.VioletDuskPeach
import com.vaultia.app.core.theme.VioletDuskPlum
import com.vaultia.app.core.theme.VioletDuskRose
import com.vaultia.app.core.theme.claymorphic
import com.vaultia.app.core.theme.claymorphicGradient

@Composable
fun ClayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseModifier = if (isPrimary) {
        modifier.claymorphicGradient(
            shape = RoundedCornerShape(16.dp),
            elevation = if (isPressed) 2.dp else 6.dp,
            brush = Brush.linearGradient(listOf(VioletDuskRose, VioletDuskPlum))
        )
    } else {
        modifier.claymorphic(
            shape = RoundedCornerShape(16.dp),
            elevation = if (isPressed) 2.dp else 4.dp,
            surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Box(
        modifier = baseModifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (isPrimary) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
