package com.example.elenchos.fakes

import com.example.elenchos.data.storage.ITestLabRepository
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TestSession
import java.io.File

class FakeTestLabRepository : ITestLabRepository {

    val apks = mutableListOf<APKArtifact>()
    val sessions = mutableListOf<TestSession>()

    override suspend fun saveApkArtifact(apk: APKArtifact) {
        apks.removeAll { it.id == apk.id }
        apks.add(0, apk)
    }

    override suspend fun getApkArtifacts(): List<APKArtifact> {
        return apks.toList()
    }

    override suspend fun deleteApkArtifact(id: String) {
        apks.removeAll { it.id == id }
    }

    override suspend fun saveTestSession(session: TestSession) {
        sessions.removeAll { it.id == session.id }
        sessions.add(0, session)
    }

    override suspend fun getTestSessions(): List<TestSession> {
        return sessions.toList()
    }

    override suspend fun getTestSession(id: String): TestSession? {
        return sessions.firstOrNull { it.id == id }
    }

    override suspend fun deleteTestSession(id: String) {
        sessions.removeAll { it.id == id }
    }

    override suspend fun clearAllData() {
        apks.clear()
        sessions.clear()
    }

    override suspend fun exportReport(session: TestSession, apk: APKArtifact, format: String): File {
        return File.createTempFile("fake_export_", ".$format")
    }
}
