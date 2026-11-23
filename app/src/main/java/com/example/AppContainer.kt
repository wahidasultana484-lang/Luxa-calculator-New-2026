package com.example

import android.content.Context
import com.example.calculator.engine.MathEngine
import com.example.history.database.LuxaDatabase
import com.example.history.repository.HistoryRepository
import com.example.settings.datastore.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val mathEngine: MathEngine by lazy { MathEngine() }

    private val database: LuxaDatabase by lazy {
        LuxaDatabase.getInstance(appContext)
    }

    val lazyHistoryRepository: Lazy<HistoryRepository> = lazy {
        HistoryRepository(database.calculationHistoryDao())
    }

    val historyRepository: HistoryRepository
        get() = lazyHistoryRepository.value

    val isHistoryDatabaseInitialized: Boolean
        get() = lazyHistoryRepository.isInitialized()

    val settingsPreferences: SettingsPreferences by lazy {
        SettingsPreferences(appContext)
    }

    init {
        settingsPreferences.warmUp(backgroundScope)
    }
}
