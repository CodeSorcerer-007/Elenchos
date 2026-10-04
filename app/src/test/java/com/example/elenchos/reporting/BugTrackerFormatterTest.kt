package com.example.elenchos.reporting

import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import org.junit.Assert.assertTrue
import org.junit.Test

class BugTrackerFormatterTest {

    private fun sampleIssue(): Issue {
        return Issue(
            id = "DEFECT-404",
            title = "NullPointerException in UserProfileFragment",
            severity = IssueSeverity.P1,
            category = IssueCategory.CRASH,
            confidence = IssueConfidence.CONFIRMED,
            affectedScreen = "UserProfileFragment",
            description = "Attempting to access user.avatarUrl when user object is null.",
            reproductionSteps = listOf("Open Profile Screen", "Simulate empty network profile response", "Crash occurs"),
            expectedBehavior = "Display placeholder avatar",
            actualBehavior = "Fatal NPE crash",
            stackTrace = "java.lang.NullPointerException: user must not be null\n\tat com.app.UserProfile.bind(UserProfile.kt:42)",
            rootCauseHypothesis = "Missing null check before view binding",
            recommendedFix = "Use safe call operator user?.avatarUrl or provide fallback default avatar"
        )
    }

    @Test
    fun formatIssueForGitHub_generatesCorrectMarkdown() {
        val issue = sampleIssue()
        val markdown = BugTrackerFormatter.formatIssueForGitHub(issue, "com.example.profile")

        assertTrue(markdown.contains("## [P1] NullPointerException in UserProfileFragment"))
        assertTrue(markdown.contains("`UserProfileFragment`"))
        assertTrue(markdown.contains("**Package:** `com.example.profile`"))
        assertTrue(markdown.contains("### Steps to Reproduce"))
        assertTrue(markdown.contains("- Open Profile Screen"))
        assertTrue(markdown.contains("```text\n${issue.stackTrace}"))
        assertTrue(markdown.contains("### Probable Root Cause"))
        assertTrue(markdown.contains("### Suggested Fix"))
        assertTrue(markdown.contains("Elenchos Mobile QA Laboratory"))
    }

    @Test
    fun formatIssueForJira_generatesCorrectJiraMarkup() {
        val issue = sampleIssue()
        val jira = BugTrackerFormatter.formatIssueForJira(issue, "com.example.profile")

        assertTrue(jira.contains("h2. [P1] NullPointerException in UserProfileFragment"))
        assertTrue(jira.contains("*Component:* {{UserProfileFragment}}"))
        assertTrue(jira.contains("*Package:* {{com.example.profile}}"))
        assertTrue(jira.contains("h3. Steps to Reproduce"))
        assertTrue(jira.contains("# Open Profile Screen"))
        assertTrue(jira.contains("{code:text}\n${issue.stackTrace}\n{code}"))
        assertTrue(jira.contains("h3. Probable Root Cause"))
        assertTrue(jira.contains("h3. Suggested Remediation"))
    }
}
