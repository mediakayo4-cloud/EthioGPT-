package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.VoiceAssistantManager
import com.example.core.attemptAutoSignIn
import com.example.core.authStateFlow
import com.example.core.signOut
import com.example.data.ai.EthioAiService
import com.example.data.local.AppPreferences
import com.example.data.model.EthioLanguage
import com.example.data.model.ThemeMode
import com.example.data.repository.EthioGptRepository
import com.example.ui.components.EthioLogoBadge
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ExploreToolsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EthioGptTheme
import com.example.ui.viewmodel.EthioGptViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_CHAT = "chat"
const val ROUTE_HISTORY = "history"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_PROFILE = "profile"
const val ROUTE_SETTINGS = "settings"

private enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHAT(ROUTE_CHAT, "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    HISTORY(ROUTE_HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    EXPLORE(ROUTE_EXPLORE, "Explore", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    PROFILE(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline),
    SETTINGS(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun EthioGptApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { AppPreferences(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val defaultLanguage by preferences.defaultLanguage.collectAsStateWithLifecycle(initialValue = EthioLanguage.ENGLISH)
    val voiceAutoSpeak by preferences.voiceAutoSpeak.collectAsStateWithLifecycle(initialValue = false)
    val onboardingCompleted by preferences.onboardingCompleted.collectAsStateWithLifecycle(initialValue = null)

    var showOnboardingOverlay by rememberSaveable { mutableStateOf(false) }
    var autoSignInAttempted by remember { mutableStateOf(false) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (!autoSignInAttempted) {
            autoSignInAttempted = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    EthioGptTheme(themeMode = themeMode) {
        val user = currentUser
        when {
            onboardingCompleted == null -> {
                Box(modifier = Modifier.fillMaxSize())
            }

            onboardingCompleted == false || showOnboardingOverlay -> {
                if (showOnboardingOverlay) {
                    BackHandler { showOnboardingOverlay = false }
                }
                OnboardingScreen(
                    onGetStarted = {
                        showOnboardingOverlay = false
                        scope.launch {
                            preferences.setOnboardingCompleted(true)
                        }
                    }
                )
            }

            user == null -> {
                AuthScreen(
                    onAuthSuccess = {},
                    onViewOnboarding = { showOnboardingOverlay = true }
                )
            }

            else -> {
                key(user.uid) {
                    AuthenticatedMainContent(
                        user = user,
                        preferences = preferences,
                        themeMode = themeMode,
                        defaultLanguage = defaultLanguage,
                        voiceAutoSpeak = voiceAutoSpeak,
                        onReplayOnboarding = { showOnboardingOverlay = true },
                        onSignOut = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedMainContent(
    user: FirebaseUser,
    preferences: AppPreferences,
    themeMode: ThemeMode,
    defaultLanguage: EthioLanguage,
    voiceAutoSpeak: Boolean,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val aiService = remember { EthioAiService() }
    val voiceManager = remember(context) { VoiceAssistantManager(context.applicationContext) }

    DisposableEffect(voiceManager) {
        onDispose {
            voiceManager.release()
        }
    }

    val viewModel: EthioGptViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = EthioGptRepository(db = db, auth = Firebase.auth)
                EthioGptViewModel(
                    repository = repository,
                    aiService = aiService,
                    preferences = preferences,
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName ?: "EthioGPT User",
                    initialEmail = user.email
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_CHAT

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        header = {
                            EthioLogoBadge(
                                size = 40.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_CHAT,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_CHAT) {
                            ChatScreen(
                                viewModel = viewModel,
                                aiService = aiService,
                                voiceManager = voiceManager,
                                voiceAutoSpeak = voiceAutoSpeak
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                viewModel = viewModel,
                                onOpenConversation = { conversation ->
                                    viewModel.openConversation(conversation)
                                    navigateToTab(ROUTE_CHAT)
                                },
                                onStartNewChat = {
                                    viewModel.startNewChat()
                                    navigateToTab(ROUTE_CHAT)
                                }
                            )
                        }
                        composable(ROUTE_EXPLORE) {
                            ExploreToolsScreen(
                                onLaunchTool = { toolMode, language, prefillPrompt ->
                                    viewModel.startNewChat(
                                        toolMode = toolMode,
                                        language = language,
                                        prefillPrompt = prefillPrompt
                                    )
                                    navigateToTab(ROUTE_CHAT)
                                }
                            )
                        }
                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                viewModel = viewModel,
                                fallbackDisplayName = user.displayName ?: "EthioGPT User",
                                fallbackEmail = user.email,
                                onOpenHistory = { navigateToTab(ROUTE_HISTORY) },
                                onOpenSettings = { navigateToTab(ROUTE_SETTINGS) },
                                onSignOutClick = onSignOut
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                themeMode = themeMode,
                                defaultLanguage = defaultLanguage,
                                voiceAutoSpeak = voiceAutoSpeak,
                                onThemeModeChange = { viewModel.setThemeMode(it) },
                                onDefaultLanguageChange = {
                                    viewModel.setLanguage(it, persistAsDefault = true)
                                },
                                onVoiceAutoSpeakChange = { viewModel.setVoiceAutoSpeak(it) },
                                onReplayOnboarding = onReplayOnboarding,
                                onSignOutClick = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
