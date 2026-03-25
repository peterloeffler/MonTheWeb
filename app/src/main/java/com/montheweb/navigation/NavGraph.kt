package com.montheweb.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.montheweb.ui.screens.addurl.AddEditUrlScreen
import com.montheweb.ui.screens.settings.SettingsScreen
import com.montheweb.ui.screens.urllist.UrlListScreen

object Routes {
    const val URL_LIST = "url_list"
    const val ADD_EDIT_URL = "add_edit_url/{urlId}"
    const val SETTINGS = "settings"

    fun addEditUrl(urlId: Long = -1L) = "add_edit_url/$urlId"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.URL_LIST
    ) {
        composable(Routes.URL_LIST) {
            UrlListScreen(
                onNavigateToAdd = { navController.navigate(Routes.addEditUrl()) },
                onNavigateToEdit = { urlId -> navController.navigate(Routes.addEditUrl(urlId)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.ADD_EDIT_URL,
            arguments = listOf(
                navArgument("urlId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            AddEditUrlScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
