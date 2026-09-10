package com.jiucaihua.app.presentation.portfolio

import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.platform.app.InstrumentationRegistry
import com.jiucaihua.app.R
import com.jiucaihua.app.domain.model.CategorySummary
import com.jiucaihua.app.domain.model.ChartRange
import com.jiucaihua.app.domain.model.Holding
import com.jiucaihua.app.domain.model.MarketType
import com.jiucaihua.app.domain.model.PortfolioSummary
import com.jiucaihua.app.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class HoldingsListTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    // MIUI blocks instrumentation's activity launch while its target app is in the background.
    // Bring the existing app forward normally; do not change system permission/settings.
    @get:Rule val rules: TestRule = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val output = automation.executeShellCommand("am start -W -n com.jiucaihua.app/.MainActivity")
            ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
        }
    }).around(compose)
    @Before fun keepTestActivityVisible() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= 27) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            }
        }
    }

    private val holdings = (0..199).map { index ->
        Holding(id = index + 1L, code = "sh$index", name = "Holding$index", marketType = MarketType.A_STOCK,
            costPrice = 1.0, holdingAmount = 100.0, holdingShares = 100.0, currentPrice = 1.2)
    }

    private fun content(restoration: StateRestorationTester, click: (String) -> Unit = {}, longClick: (Holding) -> Unit = {}) {
        val summary = PortfolioSummary(holdings = holdings,
            categorySummaries = listOf(CategorySummary(MarketType.A_STOCK, holdings = holdings)))
        restoration.setContent {
            var collapsed by rememberSaveable { mutableStateOf(emptyList<String>()) }
            MaterialTheme {
                HoldingsList(
                    listState = rememberLazyListState(), collapsedCategories = collapsed,
                    onCategoryToggle = { collapsed = if (it in collapsed) collapsed - it else collapsed + it },
                    summary = summary, periodReturns = emptyList(), sortOrder = SortOrder.DEFAULT,
                    snapshots = emptyList(), selectedChartRange = ChartRange.SEVEN_DAYS,
                    onSortChanged = {}, onHoldingClick = click, onHoldingLongClick = longClick,
                    onSetCash = {}, onSetLossCompensation = {}, onChartRangeChanged = {}, onPeriodReturnClick = {},
                )
            }
        }
    }

    @Test fun offscreenRowsAreLazyAndScrolledRowsKeepClickActions() {
        var clicked: String? = null
        var held: Holding? = null
        content(StateRestorationTester(compose), { clicked = it }, { held = it })
        // The last row must not already be composed as part of one giant category item.
        compose.onNodeWithText("Holding199").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Holding199"))
        compose.onNodeWithText("Holding199").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("sh199", clicked) }
        compose.onNodeWithText("Holding199").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(holdings.last(), held) }
    }

    @Test fun collapsedCategorySurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        content(restoration)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val collapse = context.getString(R.string.action_collapse)
        val expand = context.getString(R.string.action_expand)
        compose.onNodeWithContentDescription(collapse).performClick()
        compose.onNodeWithText("Holding0").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription(expand).assertIsDisplayed().performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Holding0"))
        compose.onNodeWithText("Holding0").assertIsDisplayed()
    }
}
