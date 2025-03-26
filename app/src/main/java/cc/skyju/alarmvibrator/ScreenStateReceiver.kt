package cc.skyju.alarmvibrator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Calendar
import java.util.Date

class ScreenStateReceiver(private val context: Context) {
    
    private val TAG = "ScreenStateReceiver"
    
    // 屏幕状态变化的广播接收器
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "屏幕点亮")
                    handleScreenOn()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "屏幕熄灭")
                    handleScreenOff()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "用户解锁")
                    // 用户解锁时重置计数
                    resetCounter()
                }
            }
        }
    }
    
    // 记录屏幕点亮/熄灭的时间戳列表
    private val screenEvents = mutableListOf<ScreenEvent>()
    
    // 超时处理器，用于重置连续操作计数
    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { resetCounter() }
    
    // 超时时间（毫秒）
    private val TIMEOUT_MS = 10000L // 10秒
    
    // 振动管理器
    private val vibrationManager = VibrationManager(context)
    
    // 闹钟信息管理器
    private val alarmInfoManager = AlarmInfoManager(context)
    
    // 唤醒锁
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 注册广播接收器
    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenReceiver, filter)
        Log.d(TAG, "屏幕状态接收器已注册")
    }
    
    // 注销广播接收器
    fun unregister() {
        try {
            context.unregisterReceiver(screenReceiver)
            Log.d(TAG, "屏幕状态接收器已注销")
        } catch (e: Exception) {
            Log.e(TAG, "注销接收器失败: ${e.message}")
        }
        handler.removeCallbacks(resetRunnable)
        releaseWakeLock()
    }
    
    // 处理屏幕点亮事件
    private fun handleScreenOn() {
        val currentTime = SystemClock.elapsedRealtime()
        screenEvents.add(ScreenEvent(true, currentTime))
        
        // 重置超时计时器
        handler.removeCallbacks(resetRunnable)
        handler.postDelayed(resetRunnable, TIMEOUT_MS)
        
        // 检查是否满足条件
        checkPattern()
    }
    
    // 处理屏幕熄灭事件
    private fun handleScreenOff() {
        val currentTime = SystemClock.elapsedRealtime()
        screenEvents.add(ScreenEvent(false, currentTime))
        
        // 重置超时计时器
        handler.removeCallbacks(resetRunnable)
        handler.postDelayed(resetRunnable, TIMEOUT_MS)
        
        // 检查是否满足条件
        checkPattern()
    }
    
    // 检查是否满足连续3次点亮熄灭的模式
    private fun checkPattern() {
        // 保留最近的事件
        if (screenEvents.size > 10) {
            screenEvents.removeAt(0)
        }
        
        if (screenEvents.size < 5) {
            return
        }
        
        val recentEvents = screenEvents.takeLast(5)
        val pattern = listOf(true, false, true, false, true)
        
        val matchesPattern = recentEvents.mapIndexed { index, event -> 
            event.isScreenOn == pattern[index] 
        }.all { it }
        
        if (matchesPattern) {
            Log.d(TAG, "检测到连续3次屏幕点亮熄灭模式")
            // 执行振动反馈
            notifyTimeToNextAlarm()
            // 重置计数器
            resetCounter()
        }
    }
    
    // 重置计数器
    private fun resetCounter() {
        screenEvents.clear()
        handler.removeCallbacks(resetRunnable)
        Log.d(TAG, "计数器已重置")
    }
    
    // 获取唤醒锁
    private fun acquireWakeLock() {
        releaseWakeLock() // 先释放之前的锁
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmVibrator:VibrationWakeLock"
        ).apply {
            acquire(3 * 60 * 1000L) // 最多持有3分钟
        }
        Log.d(TAG, "获取唤醒锁")
    }
    
    // 释放唤醒锁
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "释放唤醒锁")
            }
        }
        wakeLock = null
    }
    
    // 通过振动通知用户到下一次闹铃的时间
    @RequiresApi(Build.VERSION_CODES.S)
    private fun notifyTimeToNextAlarm() {
        // 获取唤醒锁，确保振动序列不会因为系统休眠而中断
        acquireWakeLock()
        
        // 获取下一次闹铃时间
        val alarmInfo = alarmInfoManager.getNextAlarmTime()
        
        // 如果没有设置闹铃，则振动一次表示无闹铃
        if (alarmInfo == "没有设置闹铃" || alarmInfo.startsWith("获取闹铃信息失败") || alarmInfo == "需要闹铃权限") {
            vibrationManager.vibrateLight()
            releaseWakeLock()
            return
        }
        
        try {
            // 解析闹铃时间
            val timeString = alarmInfo.substringAfter("下一个闹铃时间: ")
            val alarmTimeMs = parseTimeString(timeString)
            
            // 计算当前时间到闹铃时间的差值（毫秒）
            val currentTimeMs = System.currentTimeMillis()
            val diffMs = alarmTimeMs - currentTimeMs
            
            if (diffMs <= 0) {
                // 闹铃时间已过，振动一次表示无效
                vibrationManager.vibrateStrong()
                releaseWakeLock()
                return
            }
            
            // 转换为小时、15分钟和1分钟
            val diffMinutes = diffMs / (60 * 1000)
            val hours = diffMinutes / 60
            val remainingMinutesAfterHours = diffMinutes % 60
            val fifteenMinBlocks = remainingMinutesAfterHours / 15
            val oneMinBlocks = remainingMinutesAfterHours % 15
            
            Log.d(TAG, "到下一次闹铃: $hours 小时, $fifteenMinBlocks 个15分钟, $oneMinBlocks 分钟")
            
            // 开始连续振动序列
            startVibrationSequence(hours.toInt(), fifteenMinBlocks.toInt(), oneMinBlocks.toInt())
            
        } catch (e: Exception) {
            Log.e(TAG, "解析闹铃时间失败: ${e.message}")
            vibrationManager.vibrateStrong() // 出错时振动一次
            releaseWakeLock()
        }
    }
    
    // 开始连续振动序列
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startVibrationSequence(hours: Int, fifteenMinBlocks: Int, oneMinBlocks: Int) {
        // 首先执行小时振动
        executeHourVibrations(hours) {
            // 小时振动完成后，执行15分钟振动
            executeFifteenMinVibrations(fifteenMinBlocks) {
                // 15分钟振动完成后，执行1分钟振动
                executeOneMinVibrations(oneMinBlocks) {
                    // 所有振动完成后释放唤醒锁
                    releaseWakeLock()
                }
            }
        }
    }
    
    // 执行小时振动
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun executeHourVibrations(count: Int, onComplete: () -> Unit) {
        if (count <= 0) {
            // 如果没有小时，直接执行下一阶段
            Handler(Looper.getMainLooper()).postDelayed({
                onComplete()
            }, 500) // 短暂延迟以区分阶段
            return
        }
        
        var currentCount = 0
        val delayBetween = 500L // 每次振动之间的延迟
        
        val runnable = object : Runnable {
            override fun run() {
                if (currentCount < count) {
                    vibrationManager.vibrateStrong()
                    currentCount++
                    Handler(Looper.getMainLooper()).postDelayed(this, delayBetween)
                } else {
                    // 完成所有小时振动后，等待一段时间再开始下一阶段
                    Handler(Looper.getMainLooper()).postDelayed({
                        onComplete()
                    }, 1000)
                }
            }
        }
        
        // 开始执行振动
        Handler(Looper.getMainLooper()).post(runnable)
    }
    
    // 执行15分钟振动
    @RequiresApi(Build.VERSION_CODES.S)
    private fun executeFifteenMinVibrations(count: Int, onComplete: () -> Unit) {
        if (count <= 0) {
            // 如果没有15分钟块，直接执行下一阶段
            Handler(Looper.getMainLooper()).postDelayed({
                onComplete()
            }, 500) // 短暂延迟以区分阶段
            return
        }
        
        var currentCount = 0
        val delayBetween = 500L // 每次振动之间的延迟
        
        val runnable = object : Runnable {
            override fun run() {
                if (currentCount < count) {
                    vibrationManager.vibrateMedium()
                    currentCount++
                    Handler(Looper.getMainLooper()).postDelayed(this, delayBetween)
                } else {
                    // 完成所有15分钟振动后，等待一段时间再开始下一阶段
                    Handler(Looper.getMainLooper()).postDelayed({
                        onComplete()
                    }, 1000)
                }
            }
        }
        
        // 开始执行振动
        Handler(Looper.getMainLooper()).post(runnable)
    }
    
    // 执行1分钟振动
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun executeOneMinVibrations(count: Int, onComplete: () -> Unit) {
        if (count <= 0) {
            onComplete()
            return
        }
        
        var currentCount = 0
        val delayBetween = 500L // 每次振动之间的延迟
        
        val runnable = object : Runnable {
            override fun run() {
                if (currentCount < count) {
                    vibrationManager.vibrateLight()
                    currentCount++
                    Handler(Looper.getMainLooper()).postDelayed(this, delayBetween)
                } else {
                    onComplete()
                }
            }
        }
        
        // 开始执行振动
        Handler(Looper.getMainLooper()).post(runnable)
    }
    
    // 解析时间字符串为毫秒时间戳
    private fun parseTimeString(timeString: String): Long {
        try {
            // 格式: yyyy-MM-dd HH:mm:ss
            val parts = timeString.split(" ")
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1 // 月份从0开始
            val day = dateParts[2].toInt()
            
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val second = timeParts[2].toInt()
            
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day, hour, minute, second)
            calendar.set(Calendar.MILLISECOND, 0)
            
            return calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "解析时间字符串失败: $timeString, ${e.message}")
            throw e
        }
    }
    
    // 屏幕事件数据类
    data class ScreenEvent(val isScreenOn: Boolean, val timestamp: Long)
} 