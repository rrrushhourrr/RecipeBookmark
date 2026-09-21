package com.recipebookmark.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipebookmark.ui.detail.DetailScreen
import com.recipebookmark.ui.edit.AddMode
import com.recipebookmark.ui.edit.EditScreen
import com.recipebookmark.ui.folder.FolderScreen
import com.recipebookmark.ui.home.HomeScreen
import com.recipebookmark.ui.settings.SettingsScreen
import com.recipebookmark.ui.theme.RecipeBookmarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RecipeBookmarkTheme {
                RecipeBookmarkNavHost()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{recipeId}"
    const val EDIT = "edit?recipeId={recipeId}&mode={mode}"
    const val FOLDERS = "folders"
    const val SETTINGS = "settings"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long = 0L, mode: AddMode = AddMode.NONE) = "edit?recipeId=$id&mode=${mode.name}"
}

@Composable
private fun RecipeBookmarkNavHost() {
    val navController = rememberNavController()
    var addSheetOpen by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onOpenRecipe = { navController.navigate(Routes.detail(it)) },
                onAdd = { addSheetOpen = true },
                onOpenFolders = { navController.navigate(Routes.FOLDERS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(it)) }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = AddMode.NONE.name
                }
            )
        ) { entry ->
            val editedId = entry.arguments?.getLong("recipeId") ?: 0L
            EditScreen(
                onBack = { navController.popBackStack() },
                onDone = { savedId ->
                    // 新規作成なら、そのまま詳細へ。編集なら元の画面へ戻る。
                    if (editedId > 0L) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.detail(savedId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                }
            )
        }

        composable(Routes.FOLDERS) {
            FolderScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }

    if (addSheetOpen) {
        AddSheet(
            onSelect = { mode ->
                addSheetOpen = false
                navController.navigate(Routes.edit(mode = mode))
            },
            onDismiss = { addSheetOpen = false }
        )
    }
}
