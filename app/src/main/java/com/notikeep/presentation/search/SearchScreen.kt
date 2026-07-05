package com.notikeep.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.presentation.common.formatTimestamp

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Искать по тексту уведомлений") },
        )

        when {
            state.showNoResults -> CenterHint("Ничего не найдено")
            state.query.isBlank() -> CenterHint("Введите текст для поиска")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.results, key = { it.id }) { record ->
                    Column(Modifier.fillMaxWidth().padding(16.dp, 10.dp)) {
                        Text(record.appLabel, style = MaterialTheme.typography.labelSmall)
                        if (record.title.isNotBlank()) {
                            Text(record.title, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (record.text.isNotBlank()) {
                            Text(record.text, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(formatTimestamp(record.postedAt), style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CenterHint(text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
