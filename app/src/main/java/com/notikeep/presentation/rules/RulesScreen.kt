package com.notikeep.presentation.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notikeep.domain.model.RuleState

private val STATES = listOf(
    RuleState.SHADE_AND_ARCHIVE to "Шторка",
    RuleState.ARCHIVE_ONLY to "Архив",
    RuleState.IGNORE to "Игнор",
)

@Composable
fun RulesScreen(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Column(modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(state.rows, key = { it.packageName }) { row ->
            RuleRow(row, onSelect = { viewModel.setState(row, it) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun RuleRow(row: AppRuleRow, onSelect: (RuleState) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp, 10.dp)) {
        Text(row.label, style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            STATES.forEachIndexed { index, (ruleState, label) ->
                SegmentedButton(
                    selected = row.state == ruleState,
                    onClick = { onSelect(ruleState) },
                    shape = SegmentedButtonDefaults.itemShape(index, STATES.size),
                ) {
                    Text(label)
                }
            }
        }
    }
}
