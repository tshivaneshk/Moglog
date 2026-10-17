package com.skech.moglog.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.skech.moglog.data.auth.SignInResult
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.domain.model.Shortcut
import com.skech.moglog.ui.designsystem.ActionSheetItem
import com.skech.moglog.ui.designsystem.IosActionSheet
import com.skech.moglog.ui.designsystem.IosToastBanner
import com.skech.moglog.ui.designsystem.IosToastData
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.detail.DetailScreen
import com.skech.moglog.ui.discover.DiscoverScreen
import com.skech.moglog.ui.library.LibraryScreen
import com.skech.moglog.ui.onboarding.OnboardingScreen
import com.skech.moglog.ui.settings.PrivacyScreen
import com.skech.moglog.ui.settings.RecycleBinScreen
import com.skech.moglog.ui.settings.SettingsScreen
import com.skech.moglog.ui.stats.StatsScreen
import com.skech.moglog.ui.viewmodel.AuthViewModel
import com.skech.moglog.ui.viewmodel.DramaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MoglogNavHost(
    viewModel: DramaViewModel,
    authViewModel: AuthViewModel
) {
    val colors = LocalMoglogColors.current
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val dramas by viewModel.myDramas.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isSyncing by authViewModel.isSyncing.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isLoading.collectAsState()
    val discoverRails by viewModel.discoverRails.collectAsState()

    var isSigningIn by remember { mutableStateOf(false) }
    var pendingAuthResult by remember { mutableStateOf<SignInResult?>(null) }
    var globalToast by remember { mutableStateOf<IosToastData?>(null) }

    LaunchedEffect(globalToast) {
        if (globalToast != null) {
            delay(2800)
            globalToast = null
        }
    }

    LaunchedEffect(Unit) {
        if (settings.syncFrequency == "On App Launch" && currentUser != null) {
            authViewModel.fetchFromCloud()
        }
    }

    val performGoogleSignIn: () -> Unit = {
        scope.launch {
            isSigningIn = true
            val result = authViewModel.authClient.signIn(context)
            if (result.data != null) {
                val uid = result.data.uid
                val exists = authViewModel.checkAccountExists(uid)
                isSigningIn = false
                if (exists) {
                    authViewModel.signIn(result)
                    globalToast = IosToastData("Signed in as ${result.data.displayName ?: result.data.email ?: "user"}")
                } else {
                    pendingAuthResult = result
                }
            } else {
                isSigningIn = false
                val msg = if (result.errorMessage?.contains("No credentials") == true) {
                    "No Google Account found on this device."
                } else {
                    "Sign in cancelled or failed."
                }
                globalToast = IosToastData(msg)
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab: MainTab? = remember(currentDestination) {
        when {
            currentDestination == null -> null
            currentDestination.hasRoute<MoglogRoutes.Library>() -> MainTab.LIBRARY
            currentDestination.hasRoute<MoglogRoutes.Discover>() -> MainTab.DISCOVER
            currentDestination.hasRoute<MoglogRoutes.Stats>() -> MainTab.STATS
            currentDestination.hasRoute<MoglogRoutes.Settings>() -> MainTab.SETTINGS
            else -> null
        }
    }

    val startDestination: Any = remember {
        if (viewModel.settingsStore.settingsState.value.isFirstLaunch) {
            MoglogRoutes.Onboarding
        } else {
            MoglogRoutes.Library
        }
    }

    val navigateToTab: (MainTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(320)
                ) + fadeIn(animationSpec = tween(220))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                    animationSpec = tween(320)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -(fullWidth * 0.3f).toInt() },
                    animationSpec = tween(320)
                ) + fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(320)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) {
            composable<MoglogRoutes.Onboarding> {
                OnboardingScreen(
                    initialKey = viewModel.secureStore.getTmdbCredential(),
                    isValidKey = { viewModel.secureStore.isValidTmdbCredential(it) },
                    onSaveKey = { key ->
                        viewModel.secureStore.setTmdbCredential(key)
                        viewModel.loadDiscoverRails(forceRefresh = true)
                    },
                    onGoogleSignInClick = performGoogleSignIn,
                    isSignedIn = currentUser != null,
                    signedInEmail = currentUser?.email,
                    onFinish = {
                        scope.launch {
                            viewModel.settingsStore.setFirstLaunchCompleted()
                        }
                        navController.navigate(MoglogRoutes.Library) {
                            popUpTo(MoglogRoutes.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable<MoglogRoutes.Library> {
                LibraryScreen(
                    dramas = dramas,
                    layoutMode = settings.layoutMode,
                    gridColumns = settings.gridColumns,
                    onToggleLayoutMode = { viewModel.setLayoutMode(it) },
                    onDramaClick = { dramaId ->
                        navController.navigate(MoglogRoutes.Detail(dramaId))
                    },
                    onNavigateToDiscover = { navigateToTab(MainTab.DISCOVER) },
                    onUpdateDrama = { viewModel.saveDrama(it) },
                    onMoveToRecycleBin = { viewModel.moveToRecycleBin(it) },
                    onRestoreDrama = { viewModel.restoreDrama(it) },
                    bottomBarPadding = PaddingValues(bottom = 84.dp)
                )
            }

            composable<MoglogRoutes.Discover> {
                LaunchedEffect(hasApiKey) {
                    if (hasApiKey) {
                        viewModel.loadDiscoverRails(forceRefresh = false)
                    }
                }
                val savedIds = remember(dramas) {
                    dramas.filter { !it.isDeleted }.map { it.id }.toSet()
                }
                DiscoverScreen(
                    hasApiKey = hasApiKey,
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    recentSearches = settings.recentSearches,
                    onClearRecentSearches = { viewModel.clearRecentSearches() },
                    discoverRails = discoverRails,
                    onRefreshRails = { viewModel.loadDiscoverRails(forceRefresh = true) },
                    savedDramaIds = savedIds,
                    onDramaClick = { dramaId ->
                        navController.navigate(MoglogRoutes.Detail(dramaId))
                    },
                    onQuickAddToPlanned = { drama ->
                        viewModel.saveDrama(drama.copy(status = "Planned"))
                    },
                    onNavigateToSettings = { navigateToTab(MainTab.SETTINGS) },
                    bottomBarPadding = PaddingValues(bottom = 84.dp)
                )
            }

            composable<MoglogRoutes.Stats> {
                StatsScreen(
                    dramas = dramas,
                    onDramaClick = { dramaId ->
                        navController.navigate(MoglogRoutes.Detail(dramaId))
                    },
                    bottomBarPadding = PaddingValues(bottom = 84.dp)
                )
            }

            composable<MoglogRoutes.Settings> {
                SettingsScreen(
                    dramas = dramas,
                    settings = settings,
                    currentUser = currentUser,
                    isSyncing = isSyncing,
                    secureStore = viewModel.secureStore,
                    settingsStore = viewModel.settingsStore,
                    onSignInClick = performGoogleSignIn,
                    onSignOutClick = {
                        authViewModel.signOut()
                        globalToast = IosToastData("Signed out")
                    },
                    onSyncToCloud = {
                        authViewModel.syncToCloud(dramas)
                        scope.launch {
                            viewModel.settingsStore.setLastSyncedAt(System.currentTimeMillis())
                        }
                        globalToast = IosToastData("Cloud backup started")
                    },
                    onRestoreFromCloud = {
                        scope.launch {
                            val cloud = authViewModel.fetchFromCloud()
                            if (cloud != null) {
                                viewModel.settingsStore.setLastSyncedAt(System.currentTimeMillis())
                                globalToast = IosToastData("Restored ${cloud.dramas.size} shows from cloud")
                            } else {
                                globalToast = IosToastData("No cloud backup found or offline")
                            }
                        }
                    },
                    onDeleteAccount = { deleteLocal, cb ->
                        authViewModel.deleteAccount(deleteLocal, cb)
                    },
                    onNavigateToRecycleBin = {
                        navController.navigate(MoglogRoutes.RecycleBin)
                    },
                    onNavigateToPrivacy = {
                        navController.navigate(MoglogRoutes.Privacy)
                    },
                    onClearMetadataCache = {
                        viewModel.clearMetadataCache()
                    },
                    onExportLibrary = { uri, passphrase, onDone ->
                        viewModel.exportDramas(context, uri, passphrase, onDone)
                    },
                    onImportLibrary = { uri, passphrase, onDone ->
                        viewModel.importDramas(context, uri, passphrase, onDone)
                    },
                    onShowToast = { msg ->
                        globalToast = IosToastData(msg)
                    },
                    bottomBarPadding = PaddingValues(bottom = 84.dp)
                )
            }

            composable<MoglogRoutes.Detail> { entry ->
                val route = entry.toRoute<MoglogRoutes.Detail>()
                val existingLocal = dramas.find { it.id == route.dramaId && !it.isDeleted }
                var resolvedDrama by remember(route.dramaId, existingLocal) {
                    mutableStateOf(existingLocal)
                }
                var shortcut by remember(route.dramaId) { mutableStateOf<Shortcut?>(null) }

                LaunchedEffect(route.dramaId, existingLocal) {
                    if (existingLocal != null) {
                        resolvedDrama = existingLocal
                    } else {
                        resolvedDrama = viewModel.resolveDramaById(route.dramaId)
                    }
                    shortcut = viewModel.getShortcutForDrama(route.dramaId)
                }

                val currentDrama = resolvedDrama
                if (currentDrama == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                } else {
                    DetailScreen(
                        drama = currentDrama,
                        isAlreadyInList = existingLocal != null,
                        dynamicPosterTintEnabled = settings.dynamicPosterTint,
                        initialShortcut = shortcut,
                        onSaveShortcut = { newShortcut ->
                            shortcut = newShortcut
                            viewModel.saveShortcutForDrama(currentDrama.id, newShortcut)
                        },
                        fetchExtraDetails = { viewModel.getDramaExtraDetails(it) },
                        onBack = { navController.popBackStack() },
                        onSaveDrama = { saved ->
                            viewModel.saveDrama(saved)
                        },
                        onDeleteDrama = { toDelete ->
                            viewModel.moveToRecycleBin(toDelete)
                            navController.popBackStack()
                            globalToast = IosToastData(
                                message = "Moved \"${toDelete.title}\" to Recycle Bin",
                                actionLabel = "Undo",
                                onAction = { viewModel.restoreDrama(toDelete) }
                            )
                        },
                        onNavigateToDrama = { nextId ->
                            navController.navigate(MoglogRoutes.Detail(nextId))
                        }
                    )
                }
            }

            composable<MoglogRoutes.RecycleBin> {
                val deletedDramas = remember(dramas) { dramas.filter { it.isDeleted } }
                RecycleBinScreen(
                    deletedDramas = deletedDramas,
                    onBack = { navController.popBackStack() },
                    onRestore = { drama ->
                        viewModel.restoreDrama(drama)
                        globalToast = IosToastData("Restored \"${drama.title}\"")
                    },
                    onDeletePermanently = { drama ->
                        viewModel.deleteDrama(drama)
                    },
                    onEmptyBin = {
                        viewModel.emptyRecycleBin()
                        globalToast = IosToastData("Recycle Bin emptied")
                    }
                )
            }

            composable<MoglogRoutes.Privacy> {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }

        // Bottom 4-Tab Bar (visible on top-level tabs)
        AnimatedVisibility(
            visible = currentTab != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (currentTab != null) {
                IosBottomTabBar(
                    selectedTab = currentTab,
                    onTabSelected = navigateToTab
                )
            }
        }

        // Global Toast Banner
        IosToastBanner(
            toast = globalToast,
            onDismiss = { globalToast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (currentTab != null) 62.dp else 16.dp)
        )

        if (isSigningIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
            }
        }
    }

    var isCreatingAccount by remember { mutableStateOf(false) }

    if (pendingAuthResult != null) {
        IosActionSheet(
            title = "Create Cloud Backup Account?",
            message = "No existing Moglog cloud backup was found for this Google account. Would you like to initialize one?",
            actions = listOf(
                ActionSheetItem(
                    label = "Create Account & Enable Sync",
                    onClick = {
                        val result = pendingAuthResult
                        pendingAuthResult = null
                        isCreatingAccount = true
                        if (result != null) {
                            authViewModel.createAccountAndSync(result, dramas) { success ->
                                isCreatingAccount = false
                                if (success) {
                                    scope.launch {
                                        viewModel.settingsStore.setLastSyncedAt(System.currentTimeMillis())
                                    }
                                    globalToast = IosToastData("Cloud account created & sync enabled")
                                } else {
                                    globalToast = IosToastData("Failed to create cloud account")
                                }
                            }
                        } else {
                            isCreatingAccount = false
                        }
                    }
                )
            ),
            onDismissRequest = {
                if (!isCreatingAccount) {
                    scope.launch { authViewModel.authClient.signOut() }
                }
                pendingAuthResult = null
            }
        )
    }
}
