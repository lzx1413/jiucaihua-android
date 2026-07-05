package com.jiucaihua.app.presentation.holdings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.R
import com.jiucaihua.app.presentation.holdings.components.HoldingForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHoldingScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditHoldingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) {
                            stringResource(R.string.holding_edit_title)
                        } else {
                            stringResource(R.string.holding_add_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = uiState.isValid && !uiState.isLoading,
                    ) {
                        Text(
                            stringResource(R.string.action_save),
                            color = if (uiState.isValid) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        HoldingForm(
            state = uiState,
            onCodeChange = viewModel::onCodeChange,
            onNameChange = viewModel::onNameChange,
            onMarketTypeChange = viewModel::onMarketTypeChange,
            onCostPriceChange = viewModel::onCostPriceChange,
            onHoldingSharesChange = viewModel::onHoldingSharesChange,
            onTradeActionChange = viewModel::onTradeActionChange,
            onSelectResult = viewModel::applySearchResult,
            onDismissSearch = viewModel::dismissSearch,
            onGoldPresetSelected = viewModel::selectGoldPreset,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        )
    }
}
