package com.example.elenchos.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

data class DiscoveredUiElement(
    val id: String,
    val viewIdResourceName: String?,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val bounds: Rect,
    val accessibilityNodeInfo: AccessibilityNodeInfo? = null
)

data class UiSnapshot(
    val packageName: String,
    val activityName: String?,
    val elements: List<DiscoveredUiElement>,
    val timestamp: Long = System.currentTimeMillis()
)

class ElenchosLabAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentTarget = activeTargetPackage ?: return
        val eventPkg = event.packageName?.toString() ?: return

        // Detect crash dialogs from Android system or target
        if (eventPkg == "android" || eventPkg == "com.android.systemui" || eventPkg == currentTarget) {
            val eventText = event.text.joinToString(" ")
            if (eventText.contains("has stopped", ignoreCase = true) ||
                eventText.contains("keeps stopping", ignoreCase = true) ||
                eventText.contains("isn't responding", ignoreCase = true) ||
                eventText.contains("closed", ignoreCase = true)
            ) {
                onCrashOrAnrDetected?.invoke(eventText)
            }
        }

        if (eventPkg == currentTarget) {
            val root = rootInActiveWindow ?: return
            val elements = mutableListOf<DiscoveredUiElement>()
            traverseNode(root, elements)
            val snapshot = UiSnapshot(
                packageName = eventPkg,
                activityName = event.className?.toString(),
                elements = elements
            )
            onSnapshotCaptured?.invoke(snapshot)
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo, list: MutableList<DiscoveredUiElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val isInteractive = node.isClickable || node.isEditable || node.isScrollable ||
                node.isCheckable || (node.text?.isNotBlank() == true)

        var keepNode = false
        if (isInteractive && bounds.width() > 0 && bounds.height() > 0) {
            list.add(
                DiscoveredUiElement(
                    id = "${node.viewIdResourceName ?: node.className}_${bounds.left}_${bounds.top}",
                    viewIdResourceName = node.viewIdResourceName,
                    className = node.className?.toString() ?: "android.view.View",
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    bounds = bounds,
                    accessibilityNodeInfo = node
                )
            )
            keepNode = true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, list)
        }

        if (!keepNode) {
            safeRecycle(node)
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        if (instance == this) {
            instance = null
            activeTargetPackage = null
            onSnapshotCaptured = null
            onCrashOrAnrDetected = null
        }
    }

    companion object {
        @Volatile
        var instance: ElenchosLabAccessibilityService? = null
        val isServiceRunning: Boolean get() = instance != null

        @Volatile
        var activeTargetPackage: String? = null
        @Volatile
        var onSnapshotCaptured: ((UiSnapshot) -> Unit)? = null
        @Volatile
        var onCrashOrAnrDetected: ((String) -> Unit)? = null

        @Suppress("DEPRECATION")
        fun safeRecycle(node: AccessibilityNodeInfo?) {
            try {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    node?.recycle()
                }
            } catch (_: Exception) {}
        }

        fun isAccessibilityEnabled(context: Context): Boolean {
            return try {
                val serviceName = "${context.packageName}/${ElenchosLabAccessibilityService::class.java.name}"
                val accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
                if (accessibilityEnabled == 1) {
                    val enabledServices = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    ) ?: ""
                    enabledServices.contains(serviceName)
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.w("AccessibilityService", "Failed to check accessibility status: ${e.message}")
                false
            }
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        fun performClickElement(element: DiscoveredUiElement): Boolean {
            return try {
                element.accessibilityNodeInfo?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
            } catch (e: Exception) {
                android.util.Log.w("AccessibilityService", "Failed click element: ${e.message}")
                false
            }
        }

        fun performInputText(element: DiscoveredUiElement, text: String): Boolean {
            return try {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                element.accessibilityNodeInfo?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) ?: false
            } catch (e: Exception) {
                android.util.Log.w("AccessibilityService", "Failed input text: ${e.message}")
                false
            }
        }

        fun performScrollForward(): Boolean {
            val root = instance?.rootInActiveWindow
            return try {
                val scrolled = root?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
                scrolled
            } catch (e: Exception) {
                android.util.Log.w("AccessibilityService", "Failed scroll: ${e.message}")
                false
            } finally {
                safeRecycle(root)
            }
        }

        fun performBackAction(): Boolean {
            return try {
                instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
            } catch (e: Exception) {
                android.util.Log.w("AccessibilityService", "Failed back action: ${e.message}")
                false
            }
        }
    }
}
