package com.recipebookmark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Recipe::class, Folder::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "recipebookmark.db")
                .addCallback(SeedCallback)
                .build()

        /**
         * 初回作成時に「未分類」と初期フォルダを入れる。
         * onCreate はトランザクション内で同期的に呼ばれるので、素の SQL で書くのが確実。
         */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT INTO folders (id, name, sortOrder, isFixed) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(Folder.UNCATEGORIZED_ID, "未分類", 0, 1)
                )
                Folder.DEFAULT_NAMES.forEachIndexed { index, name ->
                    db.execSQL(
                        "INSERT INTO folders (name, sortOrder, isFixed) VALUES (?, ?, ?)",
                        arrayOf<Any>(name, index + 1, 0)
                    )
                }
            }
        }
    }
}
