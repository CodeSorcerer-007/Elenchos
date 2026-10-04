# Elenchos (ἔλεγχος)
### God-Tier Android Autonomous Mobile QA Laboratory

<p align="center">
  <img src="Logo.png" alt="Elenchos Laboratory Logo" width="160" />
</p>

> **"Upload APK → Test Everything Possible → Find Problems → Prove Them → Explain Them → Give an AI Agent Everything Needed to Fix Them."**

Elenchos is a native, mobile-first Android developer testing laboratory engineered to run directly on Android devices. It empowers developers and mobile QA engineers to import APKs, perform deep static security and manifest audits, autonomously exercise UI screens, fuzz boundary inputs, stress lifecycles, detect crashes and ANRs, and synthesize structured **AI Fix Packages** designed for autonomous AI coding agents.

---

## 🔬 Core Capabilities

### 1. APK Intelligence & Deep Ingestion
- **Cryptographic Verification:** SHA-256 integrity calculation, file size formatting, and DEX/assets inventory.
- **Manifest Architecture:** Full inspection of target SDK, minimum SDK, compile SDK, activities, services, broadcast receivers, and content providers.
- **Component Exposure:** Immediate detection of components declared with `android:exported="true"` without protecting permissions.
- **Permission Profiling:** Categorization into normal, dangerous runtime permissions (Camera, Location, Audio, Contacts), and privileged flags (`SYSTEM_ALERT_WINDOW`, `REQUEST_INSTALL_PACKAGES`).
- **Native Architecture & 16KB Page Alignment:** Validates 64-bit ABI support (`arm64-v8a`, `x86_64`) and page alignment compliance for Android 15/16.
- **Bytecode Credential Scanning:** Static scanning of compiled DEX string pools to detect hardcoded API keys, OAuth tokens, and localhost debug endpoints.

### 2. Autonomous Testing Engine & Platform Reality
- **Dual Testing Architecture:**
  1. **Autonomous Live Testing Mode:** Powered by the built-in `ElenchosLabAccessibilityService`. When enabled by the developer in Accessibility Settings, Elenchos autonomously inspects active window hierarchies, locates interactive controls, auto-taps buttons, types fuzz inputs into editable fields, scrolls containers, and traps system crash dialogs.
  2. **Simulated Sandbox Mode:** When accessibility privileges are not granted, Elenchos performs complete static security, manifest validation, cold startup latency measurement, intent routing verification, and logcat exception harvesting without fabricating results.
- **Input Robustness & Fuzzing:** Automated injection of boundary vectors including empty strings, 500/2000-character overflow strings, multi-byte UTF-8 emojis, SQL/XSS mocks, numeric boundaries (`-1`, `0`, `999999999999999999`), and malformed URIs.
- **Lifecycle & Configuration Resilience:** Background-to-foreground state preservation stress, back-button loop testing, and portrait/landscape orientation layout stability checks.
- **Crash & ANR Diagnostics:** Automatic capture, stack trace parsing, and occurrence deduplication of uncaught runtime exceptions (`NullPointerException`, `IndexOutOfBoundsException`, `OutOfMemoryError`) from system logcat buffers.

### 3. Objective Laboratory Health Scoring
Elenchos calculates a deterministic **Overall Health Score (0 - 100)** based on an audited mathematical formula:

$$\text{Health Score} = 0.25 \times \text{Stability} + 0.25 \times \text{Security} + 0.20 \times \text{Functionality} + 0.10 \times \text{Performance} + 0.10 \times \text{Accessibility} + 0.10 \times \text{Compatibility}$$

- **Stability Score (25%):** Penalties applied for confirmed P0 (Blocker) and P1 (Critical) crashes.
- **Security Score (25%):** Penalties for `android:debuggable="true"`, cleartext HTTP traffic, exposed unprotected services/receivers, and hardcoded credentials.
- **Functionality Score (20%):** Ratio of successfully discovered and exercised UI controls.
- **Performance Score (10%):** Evaluation of APK bloat, multidex footprint, and cold startup latency.
- **Accessibility Score (10%):** Audit of content descriptions and touch target ergonomics.
- **Compatibility Score (10%):** Verification of 64-bit native libraries and 16KB page alignment.

### 4. AI Fix Package & Agent Handoff
- **One-Click Handoff:** Dedicated `COPY AI FIX PROMPT` button generates a comprehensive senior engineering prompt ready for Claude, GPT, Gemini, or Antigravity.
- **Structured JSON Package:** Complete deterministic JSON schema containing project metadata, environment, health score, and deduplicated issues with reproduction steps, stack traces, root cause hypotheses, and remediation code diffs.
- **Bug Tracker Export:** Concise Markdown templates ready to paste directly into GitHub Issues or Jira tickets.

---

## 🎨 Visual Identity: Light Laboratory Aesthetic
Adhering to the specification, Elenchos features a **light-only developer laboratory aesthetic**:
- **Backgrounds:** Slate-50 off-white (`#F8FAFC`).
- **Surfaces:** Pure crisp white (`#FFFFFF`) with subtle slate-200 borders (`#E2E8F0`).
- **Typography:** Helvetica / Clean Sans-Serif with Monospace for package IDs, timestamps, and stack traces.
- **Technical Terminal Console:** Dark slate embedded console (`#0B132B`) for real-time live execution logs.
- **Brand Identity:** Integrated Greek *Elenchos* logo (`res/drawable/elenchos_logo.png`) across launcher icons and top navigation.

---

## 🏗️ Technical Architecture

```text
com.example.elenchos/
├── domain/
│   └── model/
│       └── Models.kt               # Pure immutable models: APKArtifact, TestSession, Issue, AIFixPackage, etc.
├── data/
│   ├── apk/
│   │   └── ApkParser.kt            # Deep ZIP, DEX, AXML, certificate, and secret parser
│   └── storage/
│       └── TestLabRepository.kt    # Mutex-locked local-first file & JSON persistence
├── testing/
│   ├── static/
│   │   └── StaticSecurityAnalyzer.kt  # Rule-based manifest, component, and security auditor
│   └── runtime/
│       ├── InputFuzzer.kt          # Boundary, Unicode, and numeric fuzzing vectors
│       ├── LogcatMonitor.kt        # Logcat stream reader, exception parser, and crash deduplicator
│       └── TestRunnerEngine.kt     # 14-phase automated testing orchestrator
├── service/
│   └── ElenchosLabAccessibilityService.kt  # Accessibility service for active window UI exploration
├── reporting/
│   ├── AIFixPackageGenerator.kt    # AI JSON package & senior engineer handoff prompt
│   ├── MarkdownReportGenerator.kt  # Full technical markdown report
│   ├── HtmlReportGenerator.kt      # Light-themed HTML report preview
│   └── BugTrackerFormatter.kt      # GitHub and Jira issue formatter
├── theme/
│   ├── Color.kt                    # Light laboratory palette & severity tokens
│   ├── Type.kt                     # Technical typography
│   └── Theme.kt                    # Light-only Compose theme
└── ui/
    ├── components/
    │   └── LabComponents.kt        # Badges, LabCard, HealthScoreGauge, TerminalConsoleView
    ├── screens/
    │   ├── HomeScreen.kt           # Laboratory dashboard & quick test launcher
    │   ├── ProjectsScreen.kt       # APK intelligence summary & component inspector
    │   ├── TestLabScreen.kt        # Real-time cockpit & live terminal console
    │   ├── ReportsScreen.kt        # Health score breakdown & coverage metrics
    │   ├── IssueExplorerScreen.kt  # Searchable & filterable P0-P4 issue explorer
    │   ├── AIFixPackageScreen.kt   # AI prompt handoff & JSON export
    │   ├── HistoryScreen.kt        # Historical run comparison (Run A vs Run B diff)
    │   └── SettingsScreen.kt       # Accessibility toggle, test budgets, & privacy controls
    ├── viewmodel/
    │   └── ElenchosViewModel.kt    # Central StateFlow management
    ├── Navigation.kt               # Top bar, bottom navigation, & screen routing
    └── MainActivity.kt             # Edge-to-edge Compose activity
```

---

## 🚀 Building & Testing Locally

### Prerequisites
- JDK 17 or JDK 21 (`JAVA_HOME` configured)
- Android SDK Platform 36 & Build-Tools 35.0.0+

### Assemble Debug APK
```bash
./gradlew assembleDebug
```
Output artifact: `app/build/outputs/apk/debug/app-debug.apk`

### Install on Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch Elenchos
```bash
adb shell am start -n com.example.elenchos/.MainActivity
```

---

## 🔒 Privacy & Safety Guarantee
- **100% Local-First:** No APK bytes or crash dumps are ever uploaded to cloud servers.
- **Isolated Sandbox Execution:** Target applications are tested via standard Android platform boundaries; foreign code is never executed inside the Elenchos process.
- **Data Erasure:** One-click "Clear All Test Data" instantly purges all imported artifacts, reports, and traces from private device storage.
