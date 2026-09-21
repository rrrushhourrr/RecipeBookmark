package com.recipebookmark

import android.content.Context

/** Context から各コンポーネントを取り出すための小さな入口。 */
val Context.app: RecipeBookmarkApp
    get() = applicationContext as RecipeBookmarkApp
