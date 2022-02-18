package com.wire.android.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wire.android.BuildConfig
import com.wire.android.ui.authentication.AuthScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.UserProfileScreen

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
enum class NavigationItem(
    private val primaryRoute: String,
    private val canonicalRoute: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (AnimatedVisibilityScope.(NavBackStackEntry) -> Unit),
    open val enterTransition: EnterTransition = slideInHorizontally(),
    open val exitTransition: ExitTransition = slideOutHorizontally()
) {

    Authentication(
        primaryRoute = "auth",
        canonicalRoute = "auth",
        content = { AuthScreen() }
    ),

    Home(
        primaryRoute = "home",
        canonicalRoute = "home",
        content = { HomeScreen(it.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_HOME_TAB_ITEM) { type = NavType.StringType }
        )
    ),

    Settings(
        primaryRoute = "settings",
        canonicalRoute = "settings",
        content = { SettingsScreen() },
    ),

    Support(
        primaryRoute = BuildConfig.SUPPORT_URL,
        canonicalRoute = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    UserProfile(
        primaryRoute = "user_profile",
        canonicalRoute = "user_profile/{$EXTRA_USER_ID}",
        content = { UserProfileScreen(it.arguments?.getString(EXTRA_USER_ID), hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_USER_ID) { type = NavType.StringType }
        ),
        // TODO: override right to left transition
    ),

    Conversation(
        primaryRoute = "conversation",
        canonicalRoute = "conversation/{$EXTRA_CONVERSATION_ID}",
        content = { ConversationScreen(hiltViewModel()) },
        arguments = listOf(
            navArgument(EXTRA_CONVERSATION_ID) { type = NavType.StringType }
        )
    );

    /**
     * The item theoretical route. If the route includes a route ID, this method will return the route with the placeholder
     */
    fun getCanonicalRoute() = canonicalRoute

    /**
     * The item navigational route. If the route includes a route ID, this will be specified by [extraRouteId]
     */
    fun getRoute(extraRouteId: String = ""): String =
        // if the route contains a path ID, append it to the Navigation Item route
        if (extraRouteId.isEmpty()) primaryRoute else "$primaryRoute/$extraRouteId}"

    companion object {
        const val HOME_START_TAB_ARGUMENT: String = "start_tab_index"
        const val CONVERSATION_ID_ARGUMENT: String = "conversation_id"
        //todo: remove when the sealed class as enum object access fixed! Related Ticket Number: AR-1038
        const val AUTHENTICATION_ROUTE: String = "auth"
        const val HOME_DEFAULT_START_ROUTE: String = "home/{$HOME_START_TAB_ARGUMENT}"

        @ExperimentalMaterialApi
        val globalNavigationItems = listOf(
            Authentication,
            Settings,
            Support,
            UserProfile,
            Home,
            Conversation
        )

        @OptIn(ExperimentalMaterialApi::class)
        private val map: Map<String, NavigationItem> = values().associateBy { it.canonicalRoute }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }
}

private const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
private const val EXTRA_USER_ID = "extra_user_id"
private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

fun NavigationItem.isExternalRoute() = this.getRoute().startsWith("http")
