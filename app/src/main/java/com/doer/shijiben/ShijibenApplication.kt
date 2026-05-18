package com.doer.shijiben

import android.app.Application
import com.doer.shijiben.data.AppDatabase
import com.doer.shijiben.data.EventRepository

class ShijibenApplication : Application() {
    private val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { EventRepository(database.eventDao()) }
}
