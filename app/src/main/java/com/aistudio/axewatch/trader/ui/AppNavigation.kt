package com.aistudio.axewatch.trader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.aistudio.axewatch.trader.ui.components.AxewatchTopBar
import com.aistudio.axewatch.trader.ui.theme.*

/** Routes and saveable back stacks keep screen state independent of refreshes. */
object AppRoute {
    const val MARKET = "market"
    const val PORTFOLIO = "portfolio"
    const val IPO = "ipo"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    isRefreshing: Boolean,
    lastRefreshTime: Long,
    onRefresh: () -> Unit,
    notificationRequest: Long,
    snackbarHost: @Composable () -> Unit = {},
    market: @Composable (() -> Unit) -> Unit,
    portfolio: @Composable () -> Unit,
    ipo: @Composable () -> Unit
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: AppRoute.MARKET
    fun openIpo() {
        nav.navigate(AppRoute.IPO) { launchSingleTop = true; restoreState = true }
    }
    fun backToMarket() {
        nav.popBackStack(AppRoute.MARKET, inclusive = false, saveState = true)
    }
    LaunchedEffect(notificationRequest, entry?.id) {
        if (notificationRequest > 0 && entry != null && route != AppRoute.IPO) {
            if (nav.currentDestination?.route == AppRoute.PORTFOLIO) backToMarket()
            openIpo()
        }
    }
    BackHandler(route != AppRoute.MARKET) { backToMarket() }
    Scaffold(
        containerColor = AxeDarkBg,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            if (route == AppRoute.IPO) {
                TopAppBar(
                    title = { Text("IPOs") },
                    navigationIcon = {
                        IconButton(onClick = { backToMarket() }, modifier = Modifier.testTag("ipo_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Market")
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                            if (isRefreshing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, "Refresh IPOs")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AxeDarkSurface)
                )
            } else AxewatchTopBar(isRefreshing, lastRefreshTime, onRefresh)
        },
        bottomBar = {
            if (route != AppRoute.IPO) NavigationBar(containerColor = AxeDarkSurface) {
                listOf(AppRoute.MARKET to "Market", AppRoute.PORTFOLIO to "Portfolio").forEach { (target, label) ->
                    NavigationBarItem(
                        selected = route == target,
                        onClick = {
                            nav.navigate(target) {
                                popUpTo(AppRoute.MARKET) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (target == AppRoute.MARKET) Icons.Default.ShowChart else Icons.Default.PieChart, null) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("bottom_nav_$target")
                    )
                }
            }
        },
        snackbarHost = snackbarHost
    ) { padding ->
        NavHost(nav, startDestination = AppRoute.MARKET, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(AppRoute.MARKET) { market { openIpo() } }
            composable(AppRoute.PORTFOLIO) { portfolio() }
            composable(AppRoute.IPO) { ipo() }
        }
    }
}
