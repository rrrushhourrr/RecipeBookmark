package com.recipebookmark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Folder>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<Folder>)

    @Update
    suspend fun update(folder: Folder)

    @Update
    suspend fun updateAll(folders: List<Folder>)

    @Delete
    suspend fun delete(folder: Folder)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM folders")
    suspend fun maxSortOrder(): Int

    /** 「未分類」だけは残して他を消す（バックアップの「置き換え」復元で使う）。 */
    @Query("DELETE FROM folders WHERE id != :keepId")
    suspend fun deleteAllExcept(keepId: Long)
}
