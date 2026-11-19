package com.remgose.rem

import android.app.Application
import com.remgose.rem.data.AppDatabase

class RemGoseApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    override fun onCreate() { super.onCreate(); instance = this }
    companion object { lateinit var instance: RemGoseApplication; private set }
}
