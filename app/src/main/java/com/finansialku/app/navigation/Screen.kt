package com.finansialku.app.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Main : Screen("main")
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconName: String
) {
    data object Dashboard : BottomNavItem("dashboard", "Dashboard", "dashboard")
    data object Transactions : BottomNavItem("transactions", "Transaksi", "receipt_long")
    data object RecurringBills : BottomNavItem("recurring_bills", "Tagihan", "autorenew")
    data object Settings : BottomNavItem("settings", "Pengaturan", "settings")
}
