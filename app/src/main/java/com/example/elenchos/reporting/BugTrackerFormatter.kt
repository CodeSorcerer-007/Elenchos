package com.example.elenchos.reporting

import com.example.elenchos.domain.model.Issue

object BugTrackerFormatter {

    fun formatIssueForGitHub(issue: Issue, packageName: String): String {
        return """
## [${issue.severity.code}] ${issue.title}

### Component / Screen
`${issue.affectedScreen}`

### Environment
- **Package:** `$packageName`
- **Severity:** ${issue.severity.label} (${issue.severity.code})
- **Confidence:** ${issue.confidence.label}
- **Category:** ${issue.category.label}

### Description
${issue.description}

### Steps to Reproduce
${issue.reproductionSteps.joinToString("\n") { "- $it" }}

### Expected Behavior
${issue.expectedBehavior}

### Actual Behavior
${issue.actualBehavior}

${if (!issue.stackTrace.isNullOrBlank()) "### Stack Trace\n```text\n${issue.stackTrace}\n```\n" else ""}
### Probable Root Cause
${issue.rootCauseHypothesis}

### Suggested Fix
${issue.recommendedFix}

---
*Reported automatically by [Elenchos Mobile QA Laboratory]*
        """.trimIndent()
    }

    fun formatIssueForJira(issue: Issue, packageName: String): String {
        return """
h2. [${issue.severity.code}] ${issue.title}

*Component:* {{${issue.affectedScreen}}}
*Package:* {{${packageName}}}
*Severity:* ${issue.severity.label} (${issue.severity.code})
*Category:* ${issue.category.label}

h3. Description
${issue.description}

h3. Steps to Reproduce
${issue.reproductionSteps.joinToString("\n") { "# $it" }}

h3. Expected Behavior
${issue.expectedBehavior}

h3. Actual Behavior
${issue.actualBehavior}

${if (!issue.stackTrace.isNullOrBlank()) "h3. Stack Trace\n{code:text}\n${issue.stackTrace}\n{code}\n" else ""}
h3. Probable Root Cause
${issue.rootCauseHypothesis}

h3. Suggested Remediation
${issue.recommendedFix}

----
_Reported automatically by Elenchos Mobile QA Laboratory_
        """.trimIndent()
    }
}
