package com.jiucaihua.app.presentation.market

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.MarketIndex
import com.jiucaihua.app.presentation.common.components.ErrorMessage
import com.jiucaihua.app.presentation.common.components.LoadingIndicator
import com.jiucaihua.app.presentation.market.components.IndexCardGrid
import com.jiucaihua.app.presentation.i18n.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onNavigateBack: () -> Unit,
    onIndexClick: ((MarketIndex) -> Unit)? = null,
    viewModel: MarketViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.market_overview)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.groups.isEmpty() -> {
                LoadingIndicator()
            }
            uiState.error != null && uiState.groups.isEmpty() -> {
                ErrorMessage(message = uiState.error ?: stringResource(R.string.loading_failed))
            }
            else -> {
                MarketScreenContent(
                    viewModel = viewModel,
                    onIndexClick = onIndexClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
fun MarketScreenContent(
    viewModel: MarketViewModel,
    onIndexClick: ((MarketIndex) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading && uiState.groups.isEmpty() -> {
            LoadingIndicator()
        }
        uiState.error != null && uiState.groups.isEmpty() -> {
            ErrorMessage(message = uiState.error ?: stringResource(R.string.loading_failed))
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                uiState.groups.forEach { group ->
                    Text(
                        text = group.tab.localizedLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )

                    IndexCardGrid(
                        indices = group.indices,
                        onIndexClick = { index ->
                            onIndexClick?.invoke(index)
                        },
                    )
                }
            }
        }
    }
}
