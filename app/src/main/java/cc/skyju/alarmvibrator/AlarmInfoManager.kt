package cc.skyju.alarmvibrator

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 管理闹铃信息的工具类
 */
class AlarmInfoManager(private val context: Context) {

    /**
     * 检查是否有闹铃权限
     * @return 是否有权限
     */
    fun hasAlarmPermission(): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12以下不需要特殊检查
        }
    }

    /**
     * 获取打开闹铃权限设置的Intent
     * @return 打开设置的Intent
     */
    fun getAlarmPermissionSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * 获取下一个闹铃的时间
     * @return 下一个闹铃的时间字符串，如果没有设置闹铃则返回提示信息
     */
    fun getNextAlarmTime(): String {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12及以上使用canScheduleExactAlarms检查权限
                if (!alarmManager.canScheduleExactAlarms()) {
                    return "需要闹铃权限"
                }
            }
            
            val nextAlarmClock = alarmManager.nextAlarmClock
            if (nextAlarmClock != null) {
                val triggerTime = nextAlarmClock.triggerTime
                val date = Date(triggerTime)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                "下一个闹铃时间: ${dateFormat.format(date)}"
            } else {
                "没有设置闹铃"
            }
        } catch (e: Exception) {
            "获取闹铃信息失败: ${e.message}"
        }
    }
} 