package com.example.mathapplock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.HashSet

class AppLockBackgroundService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var mathEngine: Grade8MathEngine
    private var overlayView: InterceptOverlayView? = null
    private var isOverlayShowing = false
    private var currentQuestion: MathQuestion? = null

    // Session state tracker: holds the system timestamp when target app was last solved/unlocked
    private var lastUnlockedTime: Long = 0

    // Tracks the last checked package to prevent looping overlays during transitions
    private var lastCheckedPackage: String? = null

    // In-memory cache for locked package names
    private val lockedPackagesCache = HashSet<String>()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val lockUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.mathapplock.UPDATE_LOCK_LIST") {
                updateCache()
            }
        }
    }

    private val appUnlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.mathapplock.APP_UNLOCKED") {
                lastUnlockedTime = System.currentTimeMillis()
            }
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                lastUnlockedTime = 0 // Re-arms the lock instantly when the device is locked
                lastCheckedPackage = null
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mathEngine = Grade8MathEngine(this)

        updateCache()

        // Register preference update receiver
        val filter = IntentFilter("com.example.mathapplock.UPDATE_LOCK_LIST")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lockUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(lockUpdateReceiver, filter)
        }

        // Register custom unlock broadcast receiver
        val unlockFilter = IntentFilter("com.example.mathapplock.APP_UNLOCKED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appUnlockedReceiver, unlockFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(appUnlockedReceiver, unlockFilter)
        }

        // Register system screen off receiver
        val screenFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, screenFilter)

        // Start Foreground Notification
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1001, notification)

        // Launch looping background check
        startMonitoringLoop()
    }

    private fun updateCache() {
        val sharedPrefs = getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE)
        val lockedPackages = sharedPrefs.getStringSet("locked_packages", emptySet()) ?: emptySet()
        synchronized(lockedPackagesCache) {
            lockedPackagesCache.clear()
            lockedPackagesCache.addAll(lockedPackages)
        }
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                checkForegroundApp()
                delay(500) // check every 500ms
            }
        }
    }

    private fun checkForegroundApp() {
        val fgPackage = getForegroundPackage() ?: return

        // Prevent infinite loops and duplicate checks on the same active package
        if (fgPackage == lastCheckedPackage) {
            return
        }
        lastCheckedPackage = fgPackage

        // Skip checks if the user has solved a math problem in the last 30 minutes (1,800,000 ms)
        val elapsed = System.currentTimeMillis() - lastUnlockedTime
        if (elapsed < 1800000) {
            return
        }
        
        val isLocked = synchronized(lockedPackagesCache) {
            lockedPackagesCache.contains(fgPackage)
        }

        if (isLocked) {
            showOverlay()
        }
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 10000, time)
        val event = UsageEvents.Event()
        var fgPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                fgPackage = event.packageName
            }
        }
        return fgPackage
    }

    private fun showOverlay() {
        if (isOverlayShowing) return

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.overlay_lock, null) as InterceptOverlayView
        overlayView = view

        val question = mathEngine.generateRandomQuestion()
        currentQuestion = question

        val tvTitle = view.findViewById<TextView>(R.id.tv_app_locked_title)
        val tvInstruction = view.findViewById<TextView>(R.id.tv_instruction)
        val tvQuestion = view.findViewById<TextView>(R.id.tv_math_question)
        val etAnswer = view.findViewById<EditText>(R.id.et_answer)
        val btnVerify = view.findViewById<Button>(R.id.btn_verify)
        val tvError = view.findViewById<TextView>(R.id.tv_error_message)

        tvTitle.text = getString(R.string.lock_title)
        tvInstruction.text = getString(R.string.lock_instruction)
        tvQuestion.text = question.questionText
        btnVerify.text = getString(R.string.lock_verify_btn)

        // Exit routine: clear the overlay view when back is pressed (home routing is handled by InterceptOverlayView)
        view.onBackPressedListener = {
            dismissOverlay()
        }

        btnVerify.setOnClickListener {
            val input = etAnswer.text.toString().trim()
            if (input.isNotEmpty()) {
                try {
                    val userAnswer = input.toInt()
                    if (userAnswer == question.correctAnswer) {
                        // Send unlock broadcast action to update lastUnlockedTime session timestamp
                        val unlockIntent = Intent("com.example.mathapplock.APP_UNLOCKED").apply {
                            setPackage(packageName)
                        }
                        sendBroadcast(unlockIntent)
                        dismissOverlay()
                    } else {
                        tvError.text = getString(R.string.lock_wrong_answer)
                        tvError.visibility = android.view.View.VISIBLE
                    }
                } catch (e: NumberFormatException) {
                    tvError.text = getString(R.string.lock_wrong_answer)
                    tvError.visibility = android.view.View.VISIBLE
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(view, params)
            isOverlayShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissOverlay() {
        if (!isOverlayShowing || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayView = null
            isOverlayShowing = false
            currentQuestion = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "app_lock_channel",
                "App Lock Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "app_lock_channel")
            .setContentTitle("MathAppLock Running")
            .setContentText("Monitoring locked applications")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel coroutines
        try {
            unregisterReceiver(lockUpdateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(appUnlockedReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dismissOverlay()
    }
}
