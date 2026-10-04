package com.example.elenchos.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.elenchos.ElenchosApplication
import com.example.elenchos.data.apk.ApkParser
import com.example.elenchos.domain.model.AIFixPackage
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestConfiguration
import com.example.elenchos.domain.model.TestPhaseProgress
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
import com.example.elenchos.reporting.AIFixPackageGenerator
import com.example.elenchos.service.ElenchosLabAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.elenchos.data.storage.ITestLabRepository
import com.example.elenchos.data.storage.TestLabRepository
import com.example.elenchos.testing.runtime.ITestRunnerEngine
import com.example.elenchos.testing.runtime.TestRunnerEngine
import com.example.elenchos.ui.navigation.NavigationScreen
import java.io.File
import java.io.FileOutputStream

class ElenchosViewModel(
    application: Application,
    private val repository: ITestLabRepository = (application as? ElenchosApplication)?.repository ?: TestLabRepository(application),
    private val testRunner: ITestRunnerEngine = (application as? ElenchosApplication)?.testRunnerEngine ?: TestRunnerEngine(application)
) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow(NavigationScreen.HOME)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    private val _apks = MutableStateFlow<List<APKArtifact>>(emptyList())
    val apks: StateFlow<List<APKArtifact>> = _apks.asStateFlow()

    private val _selectedApk = MutableStateFlow<APKArtifact?>(null)
    val selectedApk: StateFlow<APKArtifact?> = _selectedApk.asStateFlow()

    private val _activeSession = MutableStateFlow<TestSession?>(null)
    val activeSession: StateFlow<TestSession?> = _activeSession.asStateFlow()

    private val _sessionsHistory = MutableStateFlow<List<TestSession>>(emptyList())
    val sessionsHistory: StateFlow<List<TestSession>> = _sessionsHistory.asStateFlow()

    private val _activePhases = MutableStateFlow<List<TestPhaseProgress>>(emptyList())
    val activePhases: StateFlow<List<TestPhaseProgress>> = _activePhases.asStateFlow()

    private val _activeProgressPercent = MutableStateFlow(0)
    val activeProgressPercent: StateFlow<Int> = _activeProgressPercent.asStateFlow()

    private val _isTestingRunning = MutableStateFlow(false)
    val isTestingRunning: StateFlow<Boolean> = _isTestingRunning.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    // Filters for Issues Screen
    private val _issueSeverityFilter = MutableStateFlow<IssueSeverity?>(null)
    val issueSeverityFilter: StateFlow<IssueSeverity?> = _issueSeverityFilter.asStateFlow()

    private val _issueSearchQuery = MutableStateFlow("")
    val issueSearchQuery: StateFlow<String> = _issueSearchQuery.asStateFlow()

    private val _selectedIssue = MutableStateFlow<Issue?>(null)
    val selectedIssue: StateFlow<Issue?> = _selectedIssue.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        refreshData()
        checkAccessibilityStatus()
    }

    fun navigateTo(screen: NavigationScreen) {
        _currentScreen.value = screen
    }

    fun checkAccessibilityStatus() {
        try {
            val app = getApplication<Application>()
            _isAccessibilityEnabled.value = ElenchosLabAccessibilityService.isAccessibilityEnabled(app)
        } catch (_: Exception) {
            _isAccessibilityEnabled.value = false
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val loadedApks = repository.getApkArtifacts()
            _apks.value = loadedApks
            if (_selectedApk.value == null && loadedApks.isNotEmpty()) {
                _selectedApk.value = loadedApks.first()
            }

            val loadedSessions = repository.getTestSessions()
            _sessionsHistory.value = loadedSessions
            if (_activeSession.value == null && loadedSessions.isNotEmpty()) {
                _activeSession.value = loadedSessions.first()
            }
        }
    }

    fun selectApk(apk: APKArtifact) {
        _selectedApk.value = apk
    }

    fun selectSession(session: TestSession) {
        _activeSession.value = session
        val matchingApk = _apks.value.firstOrNull { it.id == session.apkArtifactId }
        if (matchingApk != null) {
            _selectedApk.value = matchingApk
        }
    }

    fun setIssueSeverityFilter(sev: IssueSeverity?) {
        _issueSeverityFilter.value = sev
    }

    fun setIssueSearchQuery(q: String) {
        _issueSearchQuery.value = q
    }

    fun selectIssue(issue: Issue?) {
        _selectedIssue.value = issue
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun importApkFromUri(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val importedDir = File(app.filesDir, "ImportedApks").apply { mkdirs() }
                val tempFile = File(importedDir, "import_${System.currentTimeMillis()}.apk")

                withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val artifact = ApkParser.parseApk(app, tempFile)
                repository.saveApkArtifact(artifact)
                _selectedApk.value = artifact
                refreshData()
                _toastMessage.value = "Imported ${artifact.appName} (${artifact.formattedSize})"
                navigateTo(NavigationScreen.PROJECTS)
            } catch (e: Exception) {
                _toastMessage.value = "Failed to import APK: ${e.message}"
            }
        }
    }

    fun startTestSession(config: TestConfiguration = TestConfiguration()) {
        val apk = _selectedApk.value ?: return
        if (_isTestingRunning.value) return

        viewModelScope.launch {
            _isTestingRunning.value = true
            _activeProgressPercent.value = 0
            navigateTo(NavigationScreen.TEST_LAB)

            try {
                val finalSession = testRunner.executeTestSession(apk, config) { session, phases, percent ->
                    _activeSession.value = session
                    _activePhases.value = phases
                    _activeProgressPercent.value = percent
                }
                repository.saveTestSession(finalSession)
                _activeSession.value = finalSession
                refreshData()
                _toastMessage.value = "Testing finished. Health Score: ${finalSession.healthScore?.overallScore}/100"
            } catch (e: Exception) {
                _toastMessage.value = "Test execution failed: ${e.message}"
            } finally {
                _isTestingRunning.value = false
            }
        }
    }

    fun abortCurrentTest() {
        testRunner.abortTest()
        _isTestingRunning.value = false
        _toastMessage.value = "Test session aborted by developer"
    }

    fun copyAIFixPromptToClipboard() {
        val session = _activeSession.value ?: return
        val apk = _selectedApk.value ?: _apks.value.firstOrNull { it.id == session.apkArtifactId } ?: return

        val pkg = AIFixPackageGenerator.generatePackage(session, apk)
        val prompt = AIFixPackageGenerator.generateAIAgentHandoffPrompt(pkg)

        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Elenchos AI Fix Prompt", prompt)
        clipboard.setPrimaryClip(clip)

        _toastMessage.value = "AI Fix Prompt copied to clipboard!"
    }

    fun deleteSelectedApk() {
        val apk = _selectedApk.value ?: return
        viewModelScope.launch {
            repository.deleteApkArtifact(apk.id)
            _selectedApk.value = null
            refreshData()
            _toastMessage.value = "APK removed from local laboratory"
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteTestSession(sessionId)
            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = null
            }
            refreshData()
            _toastMessage.value = "Test session deleted"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedApk.value = null
            _activeSession.value = null
            refreshData()
            _toastMessage.value = "All laboratory test data cleared"
        }
    }
}
