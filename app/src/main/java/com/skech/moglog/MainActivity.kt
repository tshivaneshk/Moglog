package com.skech.moglog

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skech.moglog.domain.model.Drama
import com.skech.moglog.ui.screens.DetailScreen
import com.skech.moglog.ui.screens.MyListScreen
import com.skech.moglog.ui.screens.SearchScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.skech.moglog.ui.theme.MoglogTheme
import com.skech.moglog.ui.viewmodel.DramaViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.skech.moglog.data.auth.GoogleAuthClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.skech.moglog.ui.viewmodel.AuthViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MoglogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoglogApp()
                }
            }
        }


    }
}

@Composable
fun MoglogApp() {
    val navController = rememberNavController()
    val viewModel: DramaViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val googleAuthClient = remember { com.skech.moglog.data.auth.GoogleAuthClient(context, com.google.firebase.auth.FirebaseAuth.getInstance()) }
    val authViewModel: com.skech.moglog.ui.viewmodel.AuthViewModel = hiltViewModel()
    var isSigningIn by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val appCoroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var pendingAuthResult by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.skech.moglog.data.auth.SignInResult?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (viewModel.apiKeyManager.getSyncFrequency() == "On App Launch" && authViewModel.currentUser.value != null) {
            authViewModel.fetchFromCloud()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (viewModel.apiKeyManager.isFirstLaunch()) "setup" else "my_list",
        enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn() },
        exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }) + androidx.compose.animation.fadeOut() },
        popEnterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }) + androidx.compose.animation.fadeIn() },
        popExitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut() }
    ) {
        composable("setup") {
            com.skech.moglog.ui.screens.SetupScreen(
                onFinishSetup = {
                    viewModel.apiKeyManager.setFirstLaunchCompleted()
                    navController.navigate("my_list") {
                        popUpTo("setup") { inclusive = true }
                    }
                },
                onSaveKey = { key ->
                    viewModel.apiKeyManager.setApiKey(key)
                }
            )
        }
        composable("my_list") {
            val dramas by viewModel.myDramas.collectAsState()
            
            
            val coroutineScope = rememberCoroutineScope()
            
            MyListScreen(
                dramas = dramas,
                authViewModel = authViewModel,
                onSignInClick = {
                    coroutineScope.launch {
                        isSigningIn = true
                        val result = googleAuthClient.signIn()
                        if (result.data != null) {
                            val uid = result.data.uid
                            val exists = authViewModel.checkAccountExists(uid)
                            if (exists) {
                                authViewModel.signIn(result)
                                android.widget.Toast.makeText(context, "Sign in successful!", android.widget.Toast.LENGTH_SHORT).show()
                                isSigningIn = false
                            } else {
                                isSigningIn = false
                                pendingAuthResult = result
                            }
                        } else {
                            val errorMsg = if (result.errorMessage?.contains("No credentials") == true) {
                                "No Google Account found on this device. Please log in to the Play Store first!"
                            } else {
                                "Sign in failed: ${result.errorMessage}"
                            }
                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                            isSigningIn = false
                        }
                    }
                },

                onDramaClick = { drama ->
                    val json = Uri.encode(Gson().toJson(drama))
                    navController.navigate("detail/$json")
                },
                onStatusChange = { id, status ->
                    viewModel.updateDramaStatus(id, status)
                },
                onUpdateDrama = { drama ->
                    viewModel.saveDrama(drama)
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToRecycleBin = {
                    navController.navigate("recycle_bin")
                },
                onNavigateToWatch = {
                    navController.navigate("watch")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("search") {
            val query by viewModel.searchQuery.collectAsState()
            val searchResults by viewModel.searchResults.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()

            SearchScreen(
                query = query,
                onQueryChange = viewModel::updateSearchQuery,
                searchResults = searchResults,
                isLoading = isLoading,
                onBack = { navController.popBackStack() },
                onDramaClick = { drama ->
                    val json = Uri.encode(Gson().toJson(drama))
                    navController.navigate("detail/$json")
                }
            )
        }
        composable("watch") {
            val watchViewModel: com.skech.moglog.ui.viewmodel.WatchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val shortcuts by watchViewModel.shortcuts.collectAsState()
            
            com.skech.moglog.ui.screens.WatchScreen(
                shortcuts = shortcuts,
                onBack = { navController.popBackStack() },
                onSaveShortcut = { watchViewModel.saveShortcut(it) },
                onDeleteShortcut = { watchViewModel.deleteShortcut(it) }
            )
        }
        composable("recycle_bin") {
            val dramas by viewModel.myDramas.collectAsState()
            val deletedDramas = dramas.filter { it.isDeleted }
            
            com.skech.moglog.ui.screens.RecycleBinScreen(
                deletedDramas = deletedDramas,
                onBack = { navController.popBackStack() },
                onRestore = { drama ->
                    viewModel.restoreDrama(drama)
                },
                onDeletePermanently = { drama ->
                    viewModel.deleteDrama(drama)
                }
            )
        }
        composable(
            "detail/{dramaJson}",
            arguments = listOf(navArgument("dramaJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val dramaJson = backStackEntry.arguments?.getString("dramaJson")
            val passedDrama = Gson().fromJson(dramaJson, Drama::class.java)
            val myDramas by viewModel.myDramas.collectAsState()
            
            val existingDrama = myDramas.find { it.id == passedDrama.id && !it.isDeleted }
            val displayDrama = existingDrama ?: passedDrama

            DetailScreen(
                drama = displayDrama,
                isAlreadyInList = existingDrama != null,
                shortcutJson = viewModel.apiKeyManager.getShortcut(displayDrama.id),
                onSaveShortcut = { json -> viewModel.apiKeyManager.setShortcut(displayDrama.id, json) },
                fetchExtraDetails = { viewModel.getDramaExtraDetails(it) },
                onBack = { navController.popBackStack() },
                onSaveDrama = { savedDrama ->
                    viewModel.saveDrama(savedDrama)
                },
                onDeleteDrama = { dramaToDelete ->
                    viewModel.moveToRecycleBin(dramaToDelete)
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            com.skech.moglog.ui.screens.SettingsScreen(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
    
    if (isSigningIn) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    if (pendingAuthResult != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingAuthResult = null },
            title = { androidx.compose.material3.Text("Create Account?") },
            text = { androidx.compose.material3.Text("Account doesn't exist. Want to create one?") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    authViewModel.signIn(pendingAuthResult!!)
                    pendingAuthResult = null
                    android.widget.Toast.makeText(context, "Account created!", android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    androidx.compose.material3.Text("Yes")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = {
                    appCoroutineScope.launch { googleAuthClient.signOut() }
                    pendingAuthResult = null
                }) {
                    androidx.compose.material3.Text("No")
                }
            }
        )
    }

}