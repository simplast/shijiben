package com.shijiben

import android.app.Application
import com.shijiben.ui.debug.DebugLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShiJiBenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            DebugLog.install(this)
        }
    }
}
