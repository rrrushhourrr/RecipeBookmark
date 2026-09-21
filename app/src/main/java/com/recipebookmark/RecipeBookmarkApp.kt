package com.recipebookmark

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.recipebookmark.backup.BackupManager
import com.recipebookmark.data.AppDatabase
import com.recipebookmark.data.RecipeRepository

/**
 * DI ライブラリを足すほどの規模ではないので、依存はここで組み立てて共有する。
 */
class RecipeBookmarkApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: RecipeRepository by lazy { RecipeRepository(this, database) }
    val backupManager: BackupManager by lazy { BackupManager(this, database) }

    /**
     * 共有シートは保存した直後に閉じて元アプリへ戻る。
     * 画像の取り込みや OGP 取得はそのあとも走り切ってほしいので、
     * Activity / ViewModel より長生きするスコープをアプリ側に置いておく。
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
