package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseSeeder
import com.example.data.repository.InvoiceRepository
import com.example.ui.InvoiceViewModel
import com.example.ui.InvoiceViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        android.webkit.WebView.enableSlowWholeDocumentDraw()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        val repository = InvoiceRepository(db)

        // Pre-seed sample corporate invoices and catalog on start
        lifecycleScope.launch {
            DatabaseSeeder.seedDatabase(db)
        }

        setContent {
            val viewModel: InvoiceViewModel = viewModel(
                factory = InvoiceViewModelFactory(repository)
            )

            val layoutDirection = if (viewModel.selectedLanguage == "fa") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                val darkTheme = when (viewModel.selectedThemeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }
                LaunchedEffect(darkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = if (darkTheme) {
                            androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        },
                        navigationBarStyle = if (darkTheme) {
                            androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        }
                    )
                }
                MyApplicationTheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val snackbarHostState = remember { SnackbarHostState() }

                        val notifyMsg = viewModel.userNotificationMessage
                        LaunchedEffect(notifyMsg) {
                            if (notifyMsg != null) {
                                snackbarHostState.showSnackbar(notifyMsg)
                                viewModel.clearNotification()
                            }
                        }

                        var showSplash by rememberSaveable { mutableStateOf(true) }

                        if (showSplash) {
                            SplashScreen(
                                isRtl = viewModel.selectedLanguage == "fa",
                                onDismiss = { showSplash = false }
                            )
                        } else {
                        // Screens that shouldn't show bottom navigation
                        val hideBottomNavRoutes = listOf(
                            "invoice_editor",
                            "invoice_editor?invoiceId={invoiceId}",
                            "invoice_preview/{invoiceId}"
                        )
                        val showBottomNav = currentRoute !in hideBottomNavRoutes

                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            bottomBar = {
                            if (showBottomNav) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.testTag("app_bottom_nav_bar")
                                    ) {
                                    val items = listOf(
                                        NavigationTabItem(
                                            route = "dashboard",
                                            label = if (viewModel.selectedLanguage == "fa") "پیشخوان" else "Dashboard",
                                            icon = Icons.Default.Dashboard,
                                            tag = "nav_dashboard"
                                        ),
                                        NavigationTabItem(
                                            route = "invoices",
                                            label = if (viewModel.selectedLanguage == "fa") "فاکتورها" else "Invoices",
                                            icon = Icons.Default.ReceiptLong,
                                            tag = "nav_invoices"
                                        ),
                                        NavigationTabItem(
                                            route = "customers",
                                            label = if (viewModel.selectedLanguage == "fa") "مشتریان" else "Clients",
                                            icon = Icons.Default.People,
                                            tag = "nav_customers"
                                        ),
                                        NavigationTabItem(
                                            route = "products",
                                            label = if (viewModel.selectedLanguage == "fa") "کالاها" else "Catalog",
                                            icon = Icons.Default.Inventory2,
                                            tag = "nav_products"
                                        ),
                                        NavigationTabItem(
                                            route = "projects",
                                            label = if (viewModel.selectedLanguage == "fa") "پروژه‌ها" else "Projects",
                                            icon = Icons.Default.FolderSpecial,
                                            tag = "nav_projects"
                                        ),
                                        NavigationTabItem(
                                            route = "reports",
                                            label = if (viewModel.selectedLanguage == "fa") "گزارشات" else "Reports",
                                            icon = Icons.Default.Leaderboard,
                                            tag = "nav_reports"
                                        ),
                                        NavigationTabItem(
                                            route = "settings",
                                            label = if (viewModel.selectedLanguage == "fa") "تنظیمات" else "Settings",
                                            icon = Icons.Default.Settings,
                                            tag = "nav_settings"
                                        )
                                    )

                                    items.forEach { item ->
                                        NavigationBarItem(
                                            selected = currentRoute == item.route,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = { Text(item.label, fontSize = 10.sp) },
                                            modifier = Modifier.testTag(item.tag)
                                        )
                                    }
                                }
                            }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (showBottomNav) innerPadding.calculateBottomPadding() else 0.dp,
                                    top = innerPadding.calculateTopPadding()
                                )
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewInvoice = {
                                        viewModel.startNewInvoice()
                                        navController.navigate("invoice_editor")
                                    },
                                    onNavigateToAddCustomer = {
                                        navController.navigate("customers")
                                    },
                                    onNavigateToAddProduct = {
                                        navController.navigate("products")
                                    },
                                    onNavigateToInvoiceDetails = { id ->
                                        navController.navigate("invoice_preview/$id")
                                    }
                                )
                            }

                            composable("invoices") {
                                InvoicesScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewInvoice = {
                                        viewModel.startNewInvoice()
                                        navController.navigate("invoice_editor")
                                    },
                                    onNavigateToInvoiceDetails = { id ->
                                        navController.navigate("invoice_preview/$id")
                                    }
                                )
                            }

                            composable(
                                route = "invoice_editor?invoiceId={invoiceId}",
                                arguments = listOf(
                                    navArgument("invoiceId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val invoiceIdParam = backStackEntry.arguments?.getString("invoiceId")
                                val invoiceId = invoiceIdParam?.toLongOrNull()
                                
                                LaunchedEffect(invoiceId) {
                                    if (invoiceId != null) {
                                        viewModel.startEditInvoice(invoiceId)
                                    }
                                }

                                InvoiceEditorScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onSaveSuccess = { savedId ->
                                        navController.popBackStack()
                                        navController.navigate("invoice_preview/$savedId")
                                    }
                                )
                            }

                            composable(
                                route = "invoice_preview/{invoiceId}",
                                arguments = listOf(
                                    navArgument("invoiceId") { type = NavType.LongType }
                                )
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                                InvoicePreviewScreen(
                                    invoiceId = id,
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToEdit = { editId ->
                                        navController.navigate("invoice_editor?invoiceId=$editId")
                                    },
                                    onNavigateToAllInvoices = {
                                        navController.navigate("invoices") {
                                            popUpTo("invoices") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("customers") {
                                CustomersScreen(viewModel = viewModel)
                            }

                            composable("products") {
                                ProductsScreen(viewModel = viewModel)
                            }

                            composable("projects") {
                                ProjectsScreen(
                                    viewModel = viewModel,
                                    onNavigateToNewInvoiceWithProject = { projCodeOrName, custId ->
                                        viewModel.startNewInvoice()
                                        viewModel.editorProjectNumber = projCodeOrName
                                        if (custId != null) {
                                            viewModel.editorCustomerId = custId
                                        }
                                        navController.navigate("invoice_editor")
                                    }
                                )
                            }


                            composable("reports") {
                                ReportsScreen(viewModel = viewModel)
                            }

                            composable("settings") {
                                SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                    }
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)
