package cc.skyju.alarmvibrator

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import java.io.BufferedReader
import java.io.InputStreamReader

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
        // 首先尝试使用 Root 方式获取
        val rootAlarmInfo = getNextAlarmTimeWithRoot()
        if (rootAlarmInfo != null) {
            return rootAlarmInfo
        }
        
        // 如果 Root 方式失败，回退到系统 API
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
    
    /**
     * 使用 Root 权限获取下一个闹铃时间
     * @return 闹铃时间字符串，如果获取失败则返回 null
     */
    private fun getNextAlarmTimeWithRoot(): String? {
        try {
            // 执行 dumpsys alarm 命令
            val process = Runtime.getRuntime().exec("su -c dumpsys alarm")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            
            // 读取命令输出
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            // 等待命令执行完成
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                return null
            }
            
            // 解析输出找到闹钟信息
            val alarmOutput = output.toString()
            
            // 使用正则表达式查找闹钟信息
            val pattern = Pattern.compile("tag=\\*walarm\\*:com\\.android\\.deskclock\\.ALARM_ALERT[\\s\\S]*?origWhen=([\\d-]+ [\\d:]+)\\.\\d+")
            val matcher = pattern.matcher(alarmOutput)
            
            if (matcher.find()) {
                val alarmTimeStr = matcher.group(1)
                try {
                    // 解析时间字符串并重新格式化
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(alarmTimeStr)
                    val formattedDate = dateFormat.format(date)
                    return "下一个闹铃时间: $formattedDate"
                } catch (e: Exception) {
                    // 如果解析失败，直接返回原始字符串
                    return "下一个闹铃时间: $alarmTimeStr"
                }
            }
            
            // 如果没有找到匹配的闹钟信息
            return "没有找到闹钟信息"
            
        } catch (e: Exception) {
            // Root 权限可能不可用或其他错误
            return null
        }
    }
} 