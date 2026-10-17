package com.skech.moglog

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.skech.moglog.ui.designsystem.LocalMoglogColors
import com.skech.moglog.ui.navigation.MoglogNavHost
import com.skech.moglog.ui.security.AppLockOverlay
import com.skech.moglog.ui.theme.MoglogTheme
import com.skech.moglog.ui.viewmodel.AuthViewModel
import com.skech.moglog.ui.viewmodel.DramaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dramaViewModel: DramaViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val settings by dramaViewModel.appSettings.collectAsState()

            LaunchedEffect(settings.hideInRecents, settings.appLockEnabled) {
                if (settings.hideInRecents || settings.appLockEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MoglogTheme(themeMode = settings.themeMode) {
                val colors = LocalMoglogColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MoglogNavHost(
                            viewModel = dramaViewModel,
                            authViewModel = authViewModel
                        )
                        AppLockOverlay(
                            appLockEnabled = settings.appLockEnabled,
                            timeoutSeconds = settings.appLockTimeoutSeconds,
                            hideInRecents = settings.hideInRecents
                        )
                    }
                }
            }
        }
    }
}