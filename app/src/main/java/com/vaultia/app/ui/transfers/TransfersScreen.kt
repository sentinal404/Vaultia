package com.vaultia.app.ui.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultia.app.core.theme.ClayPrimary
import com.vaultia.app.ui.components.ClayCard
import com.vaultia.app.ui.components.TransferItemView
import com.vaultia.app.ui.components.VaultiaBottomNav
import com.vaultia.app.ui.components.VaultiaTopAppBar
import com.vaultia.app.ui.viewmodel.TransferViewModel

@Composable
fun TransfersScreen(
    transferViewModel: TransferViewModel,
    onNavigateTab: (String) -> Unit
) {
    val transfers by transferViewModel.transfers.collectAsState()

    Scaffold(
        topBar = {
            VaultiaTopAppBar(
                title = "Transfers",
                subtitle = "${transfers.size} total tasks",
                actions = {
                    if (transfers.any { it.status.isFinished }) {
                        IconButton(onClick = { transferViewModel.clearCompleted() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear finished")
                        }
                    }
                }
            )
        },
        bottomBar = {
            VaultiaBottomNav(
                currentRoute = "transfers",
                onNavigate = onNavigateTab
            )
        }
    ) { paddingValues ->
        if (transfers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                ClayCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = ClayPrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Active Transfers",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Upload or download files to see real-time progress.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                items(transfers, key = { it.id }) { task ->
                    TransferItemView(
                        task = task,
                        onPause = { transferViewModel.pauseTransfer(it) },
                        onResume = { transferViewModel.resumeTransfer(it) },
                        onCancel = { transferViewModel.cancelTransfer(it) }
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}
