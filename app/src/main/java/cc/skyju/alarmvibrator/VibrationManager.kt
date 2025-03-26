package cc.skyju.alarmvibrator

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi

/**
 * 管理振动效果的工具类，使用线性马达特有效果
 */
class VibrationManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.Q)
    fun vibrateStrong() {
        val effect = VibrationEffect.createOneShot(250, 255)
        vibrator.vibrate(effect)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun vibrateMedium() {
        // 创建一个强力的震动效果，使用波形模式
        val timings = longArrayOf(0, 10, 20, 30, 40, 50)  // 立即开始，持续300毫秒
        val amplitudes = intArrayOf(0, 10, 20, 30, 40, 50)  // 最大强度
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)  // 不重复
        vibrator.vibrate(effect)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun vibrateLight() {
        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrator.vibrate(effect)
    }
} 