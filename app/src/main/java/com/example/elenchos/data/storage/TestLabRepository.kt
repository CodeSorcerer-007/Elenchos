package com.example.elenchos.data.storage

import android.content.Context
import android.util.Log
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

class TestLabRepository(private val context: Context) : ITestLabRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val mutex = Mutex()

    private val rootDir = File(context.filesDir, "TestLab")
    private val projectsDir = File(rootDir, "Projects").apply { mkdirs() }
    private val sessionsDir = File(rootDir, "Sessions").apply { mkdirs() }
    private val exportsDir = File(rootDir, "Exports").apply { mkdirs() }

    override suspend fun saveApkArtifact(apk: APKArtifact): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(projectsDir, "${apk.id}.json")
            file.writeText(json.encodeToString(apk))
        }
    }

    override suspend fun getApkArtifacts(): List<APKArtifact> = withContext(Dispatchers.IO) {
        mutex.withLock {
            projectsDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        json.decodeFromString<APKArtifact>(file.readText())
                    } catch (e: Exception) {
                        Log.w("TestLabRepository", "Failed decoding APKArtifact from ${file.name}: ${e.message}")
                        null
                    }
                }
                ?.sortedByDescending { it.importedTimestamp }
                ?: emptyList()
        }
    }

    override suspend fun deleteApkArtifact(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(projectsDir, "$id.json")
            if (file.exists()) {
                try {
                    val artifact = json.decodeFromString<APKArtifact>(file.readText())
                    val apkFile = File(artifact.filePath)
                    if (apkFile.exists()) {
                        apkFile.delete()
                    }
                } catch (e: Exception) {
                    Log.w("TestLabRepository", "Could not remove binary file for APK $id: ${e.message}")
                }
                file.delete()
            }
        }
    }

    override suspend fun saveTestSession(session: TestSession): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "${session.id}.json")
            file.writeText(json.encodeToString(session))
        }
    }

    override suspend fun getTestSessions(): List<TestSession> = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessionsDir.listFiles { _, name -> name.endsWith(".json") }
                ?.mapNotNull { file ->
                    try {
                        json.decodeFromString<TestSession>(file.readText())
                    } catch (e: Exception) {
                        Log.w("TestLabRepository", "Failed decoding TestSession from ${file.name}: ${e.message}")
                        null
                    }
                }
                ?.sortedByDescending { it.startTimestamp }
                ?: emptyList()
        }
    }

    override suspend fun getTestSession(id: String): TestSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) {
                try {
                    json.decodeFromString<TestSession>(file.readText())
                } catch (e: Exception) {
                    Log.w("TestLabRepository", "Failed reading TestSession $id: ${e.message}")
                    null
                }
            } else null
        }
    }

    override suspend fun deleteTestSession(id: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = File(sessionsDir, "$id.json")
            if (file.exists()) file.delete()
        }
    }

    override suspend fun clearAllData(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            rootDir.deleteRecursively()
            projectsDir.mkdirs()
            sessionsDir.mkdirs()
            exportsDir.mkdirs()
            val importedApks = File(context.filesDir, "ImportedApks")
            if (importedApks.exists()) {
                importedApks.deleteRecursively()
                importedApks.mkdirs()
            }
        }
    }

    override suspend fun exportReport(session: TestSession, apk: APKArtifact, format: String): File = withContext(Dispatchers.IO) {
        val cleanPkg = apk.packageName.replace('.', '_')
        val (fileName, content) = when (format.lowercase()) {
            "json", "ai_json" -> {
                val pkg = AIFixPackageGenerator.generatePackage(session, apk)
                "elenchos_ai_fix_${cleanPkg}_${session.id}.json" to AIFixPackageGenerator.exportJson(pkg)
            }
            "html" -> {
                "elenchos_report_${cleanPkg}_${session.id}.html" to HtmlReportGenerator.generateHtml(session, apk)
            }
            else -> {
                "elenchos_report_${cleanPkg}_${session.id}.md" to MarkdownReportGenerator.generateReport(session, apk)
            }
        }

        val targetFile = File(exportsDir, fileName)
        mutex.withLock {
            targetFile.writeText(content)
        }
        targetFile
    }
}
