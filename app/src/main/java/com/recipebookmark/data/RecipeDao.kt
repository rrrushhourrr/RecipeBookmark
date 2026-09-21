package com.recipebookmark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    /**
     * レシピは個人の蓄積なので多くても数百件。並べ替え・絞り込み・検索は
     * 動的 SQL を組み立てるよりメモリ上でやったほうが単純で壊れにくいため、
     * ここでは全件を Flow で流して ViewModel 側で加工する。
     */
    @Query("SELECT * FROM recipes")
    fun observeAll(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<Recipe>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Insert
    suspend fun insertAll(recipes: List<Recipe>)

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** フォルダを消したとき、中のレシピは「未分類」へ退避させる。 */
    @Query("UPDATE recipes SET folderId = :toFolderId, updatedAt = :now WHERE folderId = :fromFolderId")
    suspend fun moveFolder(fromFolderId: Long, toFolderId: Long, now: Long)

    @Query("UPDATE recipes SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long)

    @Query("DELETE FROM recipes")
    suspend fun deleteAll()
}
