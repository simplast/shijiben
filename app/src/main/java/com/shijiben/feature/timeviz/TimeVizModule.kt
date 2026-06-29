package com.shijiben.feature.timeviz

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * TimeViz 自包含 Hilt 模块。提供 timeviz_prefs SharedPreferences 与 TimeVizPrefs。
 * 不动 DataModule。
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeVizModule {

    @Provides
    @Singleton
    fun provideTimeVizSharedPreferences(
        @ApplicationContext ctx: Context
    ): SharedPreferences =
        ctx.getSharedPreferences("timeviz_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideTimeVizPrefs(prefs: SharedPreferences): TimeVizPrefs =
        createTimeVizPrefs(prefs)

    /** 提供 Clock（M2：注入到 TimeVizViewModel 替代墙钟）。 */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
