package com.vaultia.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayAccentEmerald
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.core.theme.ClaySecondary
import com.vaultia.app.ui.components.ClayButton
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.VaultiaBrandLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Vaultia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero App Header
            item {
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VaultiaBrandLogo(size = 80.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "VAULTIA",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ClayPrimary,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = "Private Decentralized Cloud Storage",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .background(ClayPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Version 2.0.0 (Evolution Edition)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ClayPrimary
                            )
                        }
                    }
                }
            }

            // Lead Architect & Creator Section
            item {
                Text(
                    text = "CREATOR & LEAD ARCHITECT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Rishi Shakya",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Lead Architect & Developer",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Built with ❤️ for privacy and sovereignty",
                                    fontSize = 11.sp,
                                    color = ClayAccentEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Vaultia is an open-source decentralized cloud application. Connect directly with the developer:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // GitHub Profile Button
                        ClayButton(
                            text = "GitHub Profile (@sentinal404)",
                            icon = Icons.Default.Code,
                            onClick = { openUrl("https://github.com/sentinal404") },
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // LinkedIn Profile Button
                        ClayButton(
                            text = "LinkedIn Profile",
                            icon = Icons.Default.OpenInBrowser,
                            onClick = { openUrl("https://in.linkedin.com/in/rishishakya") },
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = false
                        )
                    }
                }
            }

            // Open Source & Bug Reports Section
            item {
                Text(
                    text = "OPEN SOURCE & FEEDBACK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = ClayPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Found a Bug or Have a Suggestion?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "If you found any bug or have suggestions to make Vaultia better, you can directly open an issue on GitHub under the project repository. You can also check GitHub for the latest releases and updates.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        ClayButton(
                            text = "Open Issue on GitHub",
                            icon = Icons.Default.BugReport,
                            onClick = { openUrl("https://github.com/sentinal404/Vaultia/issues") },
                            modifier = Modifier.fillMaxWidth(),
                            isPrimary = false
                        )
                    }
                }
            }

            // Architecture & How it works
            item {
                Text(
                    text = "HOW VAULTIA WORKS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClayPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Storage,
                            title = "Telegram Private Cloud Engine",
                            description = "Uses the official Telegram Bot API with your private channel as unlimited, secure remote storage."
                        )

                        FeatureRow(
                            icon = Icons.Default.Lock,
                            title = "Hardware AES-256 GCM Security",
                            description = "Android Keystore hardware-backed encryption safeguards your credentials, bot tokens, and database locally."
                        )

                        FeatureRow(
                            icon = Icons.Default.Speed,
                            title = "Parallel Worker Bots",
                            description = "Multi-bot parallel scheduling accelerates uploads and downloads with intelligent least-loaded dispatching."
                        )

                        FeatureRow(
                            icon = Icons.Default.Sync,
                            title = "Incremental Auto-Backup",
                            description = "Monitors device folders and backs up only newly added files without wasting bandwidth on duplicates."
                        )
                    }
                }
            }

            item {
                Text(
                    text = "© 2026 Rishi Shakya • Vaultia v2.0.0 (Evolution Edition)",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ClayPrimary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ClayPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 16.sp
            )
        }
    }
}
