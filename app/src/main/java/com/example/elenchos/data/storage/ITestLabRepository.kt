package com.example.elenchos.data.storage

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TestSession
import java.io.File

/**
 * Contract for local-first persistence and report exports in Elenchos QA Laboratory.
 */
interface ITestLabRepository {
    suspend fun saveApkArtifact(apk: APKArtifact)
    suspend fun getApkArtifacts(): List<APKArtifact>
    suspend fun deleteApkArtifact(id: String)
    suspend fun saveTestSession(session: TestSession)
    suspend fun getTestSessions(): List<TestSession>
    suspend fun getTestSession(id: String): TestSession?
    suspend fun deleteTestSession(id: String)
    suspend fun clearAllData()
    suspend fun exportReport(session: TestSession, apk: APKArtifact, format: String): File
}
