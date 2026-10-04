package com.example.elenchos.data.storage

import android.content.Context
import com.example.elenchos.domain.model.AIFixPackage
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.reporting.AIFixPackageGenerator
import com.example.elenchos.reporting.HtmlReportGenerator
import com.example.elenchos.reporting.MarkdownReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TestLabRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val mutex = Mutex()

    private val rootDir = File(context.filesDir, "TestLab")
    private val projectsDir = File(rootDir, "Projects").apply { mkdirs() }
    private val sessionsDir = File(rootDir, "Sessions").apply { mkdirs() }
    private val exportsDir = File(rootDir, "Exports").apply { mkdirs() }

    suspend fun saveApkArtifact(apk: APKArtifact) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(projectsDir, "${apk.id}.json")
            file.writeText(json.encodeToString(apk))
        }
    }

    suspend fun getApkArtifacts(): List<APKArtifact> = withContext(Dispatchers.IO) {
        mutex.withLock {
            projectsDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        json.decodeFromString<APKArtifact>(file.readText())
                    } catch (_: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.importedTimestamp }
                ?: emptyList()
        }
    }

    suspend fun deleteApkArtifact(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(projectsDir, "$id.json")
            if (file.exists()) file.delete()
        }
    }

    suspend fun saveTestSession(session: TestSession) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "${session.id}.json")
            file.writeText(json.encodeToString(session))
        }
    }

    suspend fun getTestSessions(): List<TestSession> = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessionsDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        json.decodeFromString<TestSession>(file.readText())
                    } catch (_: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.startTimestamp }
                ?: emptyList()
        }
    }

    suspend fun getTestSession(id: String): TestSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) {
                try {
                    json.decodeFromString<TestSession>(file.readText())
                } catch (_: Exception) {
                    null
                }
            } else null
        }
    }

    suspend fun deleteTestSession(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) file.delete()
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        mutex.withLock {
            rootDir.deleteRecursively()
            projectsDir.mkdirs()
            sessionsDir.mkdirs()
            exportsDir.mkdirs()
        }
    }

    suspend fun exportReport(session: TestSession, apk: APKArtifact, format: String): File = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cleanPkg = apk.packageName.replace('.', '_')
            val file = when (format.lowercase()) {
                "json", "ai_json" -> {
                    val pkg = AIFixPackageGenerator.generatePackage(session, apk)
                    val f = File(exportsDir, "elenchos_ai_fix_${cleanPkg}_${session.id}.json")
                    f.writeText(AIFixPackageGenerator.exportJson(pkg))
                    f
                }
                "html" -> {
                    val f = File(exportsDir, "elenchos_report_${cleanPkg}_${session.id}.html")
                    f.writeText(HtmlReportGenerator.generateHtml(session, apk))
                    f
                }
                else -> {
                    val f = File(exportsDir, "elenchos_report_${cleanPkg}_${session.id}.md")
                    f.writeText(MarkdownReportGenerator.generateReport(session, apk))
                    f
                }
            }
            file
        }
    }
}
