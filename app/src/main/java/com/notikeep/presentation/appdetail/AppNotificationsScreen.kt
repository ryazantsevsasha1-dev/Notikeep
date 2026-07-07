package com.notikeep.presentation.appdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.NotificationRecord
import com.notikeep.presentation.common.AppIconImage
import com.notikeep.presentation.common.formatTimestamp
import com.notikeep.presentation.theme.SilencedAmber

/** All notifications of one app, newest first, with favorite toggles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppNotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconImage(viewModel.packageName, size = 32.dp)
                        Text(
                            state.appLabel,
                            modifier = Modifier.padding(start = 10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                Arrangement.Center,
                Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.notifications, key = { it.id }) { record ->
                NotificationRow(record, onToggleFavorite = { viewModel.toggleFavorite(record) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun NotificationRow(record: NotificationRecord, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (record.title.isNotBlank()) {
                Text(record.title, style = MaterialTheme.typography.bodyLarge)
            }
            if (record.text.isNotBlank()) {
                Text(
                    record.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatTimestamp(record.postedAt), style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onToggleFavorite) {
            if (record.isFavorite) {
                Icon(Icons.Filled.Star, contentDescription = "Убрать из избранного", tint = SilencedAmber)
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = "В избранное")
            }
        }
    }
}
