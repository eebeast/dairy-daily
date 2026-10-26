package com.milksummer.tracker

import android.app.KeyguardManager
import android.app.Activity
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.ExternalResource

/** Prepare the device before the Compose rule launches its activity. */
class UnlockedDeviceRule : ExternalResource() {
    private var originalTimeout: String? = null
    private val addedWindowFlags = mutableMapOf<Activity, Int>()
    private var monitoring = false

    @Suppress("DEPRECATION")
    private val lifecycleCallback = ActivityLifecycleCallback { activity, stage ->
        if (stage == Stage.CREATED) {
            val requested = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            addedWindowFlags[activity] = requested and activity.window.attributes.flags.inv()
            activity.window.addFlags(requested)
        }
    }

    override fun before() {
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(lifecycleCallback)
                monitoring = true
            }
            originalTimeout = shell("settings get system screen_off_timeout")
            shell("settings put system screen_off_timeout 600000")
            shell("input keyevent KEYCODE_WAKEUP")
            shell("wm dismiss-keyguard")

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val power = context.getSystemService(PowerManager::class.java)
            val keyguard = context.getSystemService(KeyguardManager::class.java)
            val deadline = SystemClock.uptimeMillis() + 5000
            while ((!power.isInteractive || keyguard.isKeyguardLocked) && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(50)
            }
            check(power.isInteractive) {
                "UI tests require an awake device. Turn on the emulator screen and rerun."
            }
            check(!keyguard.isKeyguardLocked) {
                "UI tests require an unlocked device. Unlock the emulator or phone and rerun."
            }
        } catch (error: Throwable) {
            restoreDevice()
            throw error
        }
    }

    override fun after() { restoreDevice() }

    private fun restoreDevice() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            if (monitoring) {
                ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(lifecycleCallback)
                monitoring = false
            }
            addedWindowFlags.forEach { (activity, flags) ->
                if (!activity.isDestroyed) activity.window.clearFlags(flags)
            }
            addedWindowFlags.clear()
        }
        restoreTimeout()
    }

    private fun restoreTimeout() {
        val timeout = originalTimeout ?: return
        originalTimeout = null
        if (timeout.toLongOrNull() != null) shell("settings put system screen_off_timeout $timeout")
        else if (timeout == "null") shell("settings delete system screen_off_timeout")
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
}
