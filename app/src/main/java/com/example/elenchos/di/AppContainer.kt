package com.example.elenchos.di

import android.content.Context
import com.example.elenchos.data.storage.ITestLabRepository
import com.example.elenchos.data.storage.TestLabRepository
import com.example.elenchos.testing.runtime.ITestRunnerEngine
import com.example.elenchos.testing.runtime.TestRunnerEngine

/**
 * Dependency container providing core laboratory services and repositories.
 */
interface AppContainer {
    val repository: ITestLabRepository
    val testRunnerEngine: ITestRunnerEngine
}

/**
 * Default production implementation of [AppContainer] using Android context.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    override val repository: ITestLabRepository by lazy {
        TestLabRepository(context)
    }

    override val testRunnerEngine: ITestRunnerEngine by lazy {
        TestRunnerEngine(context)
    }
}
