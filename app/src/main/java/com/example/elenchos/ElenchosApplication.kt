package com.example.elenchos

import android.app.Application
import com.example.elenchos.data.storage.ITestLabRepository
import com.example.elenchos.di.AppContainer
import com.example.elenchos.di.DefaultAppContainer
import com.example.elenchos.testing.runtime.ITestRunnerEngine

class ElenchosApplication : Application() {

    lateinit var container: AppContainer
        private set

    val repository: ITestLabRepository get() = container.repository
    val testRunnerEngine: ITestRunnerEngine get() = container.testRunnerEngine

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer(this)
    }

    companion object {
        lateinit var instance: ElenchosApplication
            private set
    }
}
