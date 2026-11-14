package com.censustracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.censustracker.ui.theme.CensusTrackerTheme
import com.censustracker.preferences.AppPreferences
import com.censustracker.ui.screen.AppStartupScreen
import com.censustracker.ui.screen.BulkSmsScreen
import com.censustracker.ui.screen.CollectorSetupScreen
import com.censustracker.ui.screen.ContactListScreen
import com.censustracker.ui.screen.AddContactScreen
import com.censustracker.ui.screen.ChatScreen
import com.censustracker.ui.screen.ErrorLogScreen
import com.censustracker.ui.screen.FailedMessagesScreen
import com.censustracker.ui.screen.PresetMessageScreen
import com.censustracker.ui.screen.SettingsScreen
import com.censustracker.ui.viewmodel.CollectorSetupViewModel
import com.censustracker.ui.viewmodel.ContactListViewModel
import com.censustracker.ui.viewmodel.AddContactViewModel
import com.censustracker.ui.viewmodel.BulkSmsViewModel
import com.censustracker.ui.viewmodel.ChatViewModel
import com.censustracker.ui.viewmodel.ErrorLogViewModel
import com.censustracker.ui.viewmodel.FailedMessagesViewModel
import com.censustracker.ui.viewmodel.PresetMessageViewModel
import com.censustracker.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CensusTrackerApp()
        }
    }
}

@Composable
fun CensusTrackerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    var darkMode by remember { mutableStateOf(appPreferences.darkModeEnabled) }

    CensusTrackerTheme(darkTheme = darkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(navController = navController, startDestination = Screen.StartupCheck) {
                // Static Security & Permission Check Entry Point
                composable(route = Screen.StartupCheck) {
                    AppStartupScreen(
                        onAllChecksPassed = {
                            navController.navigate(Screen.CollectorSetup) {
                                popUpTo(Screen.StartupCheck) { inclusive = true }
                            }
                        }
                    )
                }

                composable(route = Screen.CollectorSetup) {
                    val viewModel: CollectorSetupViewModel = hiltViewModel()
                    CollectorSetupScreen(viewModel = viewModel, onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.CollectorSetup) { inclusive = true }
                        }
                    })
                }

                composable(route = Screen.ContactList) {
                    val viewModel: ContactListViewModel = hiltViewModel()
                    ContactListScreen(
                        viewModel = viewModel,
                        onAddContact = { navController.navigate(Screen.AddContact) },
                        onContactClick = { contactId -> navController.navigate("${Screen.Chat}/$contactId") },
                        onOpenSettings = { navController.navigate(Screen.Settings) },
                        onOpenPresets = { navController.navigate(Screen.PresetMessage) },
                        onOpenErrorLog = { navController.navigate(Screen.ErrorLog) },
                        onOpenBroadcast = { navController.navigate(Screen.Broadcast) },
                        onOpenFailedMessages = { navController.navigate(Screen.FailedMessages) },
                        isDarkMode = darkMode,
                        onToggleTheme = {
                            darkMode = !darkMode
                            appPreferences.darkModeEnabled = darkMode
                        }
                    )
                }

                composable(route = Screen.AddContact) {
                    val viewModel: AddContactViewModel = hiltViewModel()
                    AddContactScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }

                composable(route = "${Screen.Chat}/{contactId}") { entry ->
                    val contactId: Long = entry.arguments?.getString("contactId")?.toLongOrNull() ?: 0L
                    val viewModel: ChatViewModel = hiltViewModel()
                    ChatScreen(viewModel = viewModel, contactId = contactId, onBack = { navController.popBackStack() })
                }

                composable(route = Screen.PresetMessage) {
                    val viewModel: PresetMessageViewModel = hiltViewModel()
                    PresetMessageScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }

                composable(route = Screen.ErrorLog) {
                    val viewModel: ErrorLogViewModel = hiltViewModel()
                    ErrorLogScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }

                composable(route = Screen.Settings) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        isDarkMode = darkMode,
                        onToggleTheme = {
                            darkMode = !darkMode
                            appPreferences.darkModeEnabled = darkMode
                        },
                        onNavigate = { route -> navController.navigate(route) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(route = Screen.Broadcast) {
                    val viewModel: BulkSmsViewModel = hiltViewModel()
                    BulkSmsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }

                composable(route = Screen.FailedMessages) {
                    val viewModel: FailedMessagesViewModel = hiltViewModel()
                    FailedMessagesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

object Screen {
    const val StartupCheck = "startup_check"
    const val CollectorSetup = "collector_setup"
    const val ContactList = "contact_list"
    const val AddContact = "add_contact"
    const val Chat = "chat"
    const val PresetMessage = "preset_messages"
    const val ErrorLog = "error_log"
    const val Settings = "settings"
    const val Broadcast = "broadcast_sms"
    const val FailedMessages = "failed_messages"
}
