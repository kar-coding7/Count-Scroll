package com.kiru.countscroll

import android.app.Application
import com.kiru.countscroll.data.local.AppDatabase
import com.kiru.countscroll.data.local.SettingsDataStore

class CountScrollApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
}
