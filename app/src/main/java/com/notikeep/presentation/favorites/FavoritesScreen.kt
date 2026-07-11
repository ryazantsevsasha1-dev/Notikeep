package com.notikeep.presentation.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.R
import com.notikeep.domain.model.AppArchiveSummary
import com.notikeep.presentation.archive.DeleteAppDialog
import com.notikeep.presentation.common.AppSummaryListItem

/** Same structure as the Archive tab, restricted to starred notifications. */
@Composable
fun FavoritesScreen(
    onOpenApp: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<AppArchiveSummary?>(null) }

    when {
        state.loading -> Column(
            modifier.fillMaxSize(),
            Arrangement.Center,
            Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }

        state.isEmpty -> Column(
            modifier.fillMaxSize().padding(32.dp),
            Arrangement.Center,
            Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.favorites_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        else -> LazyColumn(modifier.fillMaxSize()) {
            items(state.summaries, key = { it.packageName }) { summary ->
                AppSummaryListItem(
                    summary,
                    onClick = { onOpenApp(summary.packageName) },
                    onLongClick = { pendingDelete = summary },
                )
                HorizontalDivider()
            }
        }
    }

    pendingDelete?.let { summary ->
        DeleteAppDialog(
            appLabel = summary.appLabel,
            onConfirm = {
                viewModel.deleteApp(summary.packageName)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
