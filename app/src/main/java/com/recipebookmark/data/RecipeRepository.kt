package com.recipebookmark.data

import android.content.Context
import androidx.room.withTransaction
import com.recipebookmark.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RecipeRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val recipeDao = db.recipeDao()
    private val folderDao = db.folderDao()

    fun observeRecipes(): Flow<List<Recipe>> = recipeDao.observeAll()
    fun observeFolders(): Flow<List<Folder>> = folderDao.observeAll()
    fun observeRecipe(id: Long): Flow<Recipe?> = recipeDao.observeById(id)

    suspend fun getRecipe(id: Long): Recipe? = recipeDao.getById(id)

    suspend fun addRecipe(recipe: Recipe): Long =
        recipeDao.insert(recipe.copy(id = 0, createdAt = now(), updatedAt = now()))

    suspend fun updateRecipe(recipe: Recipe) =
        recipeDao.update(recipe.copy(updatedAt = now()))

    suspend fun setFavorite(id: Long, favorite: Boolean) =
        recipeDao.setFavorite(id, favorite, now())

    /** レシピと一緒に、そのレシピだけが使っている画像ファイルも消す。 */
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteById(recipe.id)
        withContext(Dispatchers.IO) {
            val stillUsed = recipeDao.getAll()
                .flatMap { it.imagePaths + listOfNotNull(it.thumbnailPath) }
                .toSet()
            val owned = (recipe.imagePaths + listOfNotNull(recipe.thumbnailPath)).toSet()
            ImageStore.deleteAll(context, owned - stillUsed)
        }
    }

    // --- フォルダ -----------------------------------------------------------

    suspend fun addFolder(name: String): Long {
        val order = folderDao.maxSortOrder() + 1
        return folderDao.insert(Folder(name = name.trim(), sortOrder = order))
    }

    suspend fun renameFolder(folder: Folder, newName: String) {
        if (folder.isFixed) return
        folderDao.update(folder.copy(name = newName.trim()))
    }

    /** 画面上の並び順をそのまま sortOrder に焼き直す。 */
    suspend fun reorderFolders(ordered: List<Folder>) {
        folderDao.updateAll(ordered.mapIndexed { index, f -> f.copy(sortOrder = index) })
    }

    /** 削除したフォルダの中身は「未分類」へ移す。 */
    suspend fun deleteFolder(folder: Folder) {
        if (folder.isFixed) return
        db.withTransaction {
            recipeDao.moveFolder(folder.id, Folder.UNCATEGORIZED_ID, now())
            folderDao.delete(folder)
        }
    }

    suspend fun moveRecipeToFolder(recipe: Recipe, folderId: Long) =
        recipeDao.update(recipe.copy(folderId = folderId, updatedAt = now()))

    private fun now() = System.currentTimeMillis()
}
