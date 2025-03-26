package cc.skyju.alarmvibrator

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants

/**
 * 管理振动效果的工具类，使用线性马达特有效果
 */
class VibrationManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 执行强烈振动 (1小时提醒) - 使用线性马达的"哒哒哒"效果
     */
    fun vibrateStrong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用预定义效果
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            // 使用Handler延迟执行，避免阻塞主线程
            vibrator.vibrate(effect)
            
            // 使用Handler延迟执行多次振动
            for (i in 1..4) {
                handler.postDelayed({
                    vibrator.vibrate(effect)
                }, i * 150L)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8-9使用自定义波形
            // 使用线性马达特有的短促振动模式
            val timings = longArrayOf(0, 50, 100, 50, 100, 50, 100, 50, 100, 50)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255) // 最大强度
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            // 旧版本Android使用传统振动
            vibrator.vibrate(longArrayOf(0, 50, 100, 50, 100, 50, 100, 50, 100, 50), -1)
        }
    }

    /**
     * 执行中等强度振动 (15分钟提醒) - 使用线性马达的中等"哒哒"效果
     */
    fun vibrateMedium() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用预定义效果
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
            
            // 使用Handler延迟执行多次振动
            for (i in 1..2) {
                handler.postDelayed({
                    vibrator.vibrate(effect)
                }, i * 200L)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8-9使用自定义波形
            val timings = longArrayOf(0, 40, 150, 40, 150, 40)
            val amplitudes = intArrayOf(0, 180, 0, 180, 0, 180) // 中等强度
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 150, 40, 150, 40), -1)
        }
    }

    /**
     * 执行轻微振动 (1分钟提醒) - 使用线性马达的轻微"哒"效果
     */
    fun vibrateLight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用预定义效果
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8-9使用自定义波形
            // 单次短促振动
            vibrator.vibrate(VibrationEffect.createOneShot(30, 80)) // 轻微强度
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
} 