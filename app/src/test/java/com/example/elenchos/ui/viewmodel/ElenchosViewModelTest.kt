package com.example.elenchos.ui.viewmodel

import android.app.Application
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.fakes.FakeTestLabRepository
import com.example.elenchos.fakes.FakeTestRunnerEngine
import com.example.elenchos.ui.navigation.NavigationScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ElenchosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTestLabRepository
    private lateinit var fakeRunner: FakeTestRunnerEngine
    private lateinit var fakeApplication: Application

    private fun sampleApk(id: String = "apk-1", name: String = "Test App"): APKArtifact {
        return APKArtifact(
            id = id,
            filePath = "/fake/path/$name.apk",
            appName = name,
            packageName = "com.test.$id",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 35,
            compileSdk = 35,
            fileSizeBytes = 1024 * 1024,
            formattedSize = "1.0 MB",
            sha256 = "hash_$id",
            isDebuggable = false,
            allowsBackup = false,
            usesCleartextTraffic = false,
            is16KbPageAligned = true,
            isInstalledOnDevice = true,
            dexCount = 1,
            totalAssetsCount = 0,
            nativeArchitectures = emptyList(),
            permissions = emptyList(),
            dangerousPermissions = emptyList(),
            activities = emptyList(),
            services = emptyList(),
            receivers = emptyList(),
            providers = emptyList(),
            detectedSecrets = emptyList()
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTestLabRepository()
        fakeRunner = FakeTestRunnerEngine()
        fakeApplication = Application()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialScreen_isHome() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        assertEquals(NavigationScreen.HOME, viewModel.currentScreen.value)
    }

    @Test
    fun navigateTo_updatesCurrentScreen() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        viewModel.navigateTo(NavigationScreen.TEST_LAB)
        assertEquals(NavigationScreen.TEST_LAB, viewModel.currentScreen.value)

        viewModel.navigateTo(NavigationScreen.REPORTS)
        assertEquals(NavigationScreen.REPORTS, viewModel.currentScreen.value)
    }

    @Test
    fun selectApk_updatesSelectedApkState() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        val apk = sampleApk()
        viewModel.selectApk(apk)
        assertEquals(apk, viewModel.selectedApk.value)
    }

    @Test
    fun filterAndSearch_updatesFlowStates() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        assertNull(viewModel.issueSeverityFilter.value)
        assertEquals("", viewModel.issueSearchQuery.value)

        viewModel.setIssueSeverityFilter(IssueSeverity.P0)
        assertEquals(IssueSeverity.P0, viewModel.issueSeverityFilter.value)

        viewModel.setIssueSearchQuery("buffer overflow")
        assertEquals("buffer overflow", viewModel.issueSearchQuery.value)
    }

    @Test
    fun clearToast_resetsToastState() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, repository = fakeRepository, testRunner = fakeRunner)
        viewModel.clearToast()
        assertNull(viewModel.toastMessage.value)
    }

    @Test
    fun startTestSession_runsEngineAndUpdatesState() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        val apk = sampleApk()
        viewModel.selectApk(apk)

        viewModel.startTestSession()
        advanceUntilIdle()

        assertEquals(1, fakeRunner.executionCount)
        assertNotNull(viewModel.activeSession.value)
        assertFalse(viewModel.isTestingRunning.value)
        assertEquals("Test App", viewModel.activeSession.value?.appName)
    }

    @Test
    fun abortCurrentTest_triggersRunnerAbort() = runTest(testDispatcher) {
        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        viewModel.abortCurrentTest()
        assertTrue(fakeRunner.wasAborted)
        assertFalse(viewModel.isTestingRunning.value)
        assertNotNull(viewModel.toastMessage.value)
    }

    @Test
    fun clearAllData_clearsRepositoryAndResetState() = runTest(testDispatcher) {
        val apk = sampleApk()
        fakeRepository.saveApkArtifact(apk)

        val viewModel = ElenchosViewModel(fakeApplication, fakeRepository, fakeRunner)
        advanceUntilIdle()

        viewModel.clearAllData()
        advanceUntilIdle()

        assertTrue(fakeRepository.apks.isEmpty())
        assertTrue(fakeRepository.sessions.isEmpty())
        assertNull(viewModel.selectedApk.value)
        assertNull(viewModel.activeSession.value)
    }
}
