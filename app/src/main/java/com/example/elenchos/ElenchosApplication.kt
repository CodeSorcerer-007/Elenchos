package com.example.elenchos

import android.app.Application
import com.example.elenchos.data.storage.TestLabRepository
import com.example.elenchos.testing.runtime.TestRunnerEngine

class ElenchosApplication : Application() {

    lateinit var repository: TestLabRepository
        private set

    lateinit var testRunnerEngine: TestRunnerEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = TestLabRepository(this)
        testRunnerEngine = TestRunnerEngine(this)
    }

    companion object {
        lateinit var instance: ElenchosApplication
            private set
    }
}
