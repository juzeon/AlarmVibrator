package cc.skyju.alarmvibrator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cc.skyju.alarmvibrator.ui.theme.AlarmVibratorTheme

class MainActivity : ComponentActivity() {
    private lateinit var alarmInfoManager: AlarmInfoManager
    private lateinit var vibrationManager: VibrationManager
    
    // 请求通知权限的启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限获取成功，启动前台服务
            startForegroundService()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化管理器
        alarmInfoManager = AlarmInfoManager(this)
        vibrationManager = VibrationManager(this)
        
        // 检查并请求通知权限
        checkNotificationPermission()
        
        setContent {
            AlarmVibratorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmInfoScreen(
                        modifier = Modifier.padding(innerPadding),
                        initialAlarmInfo = alarmInfoManager.getNextAlarmTime(), // 应用启动时获取闹铃信息
                        vibrationManager = vibrationManager
                    )
                }
            }
        }
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，启动前台服务
                    startForegroundService()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 可以在这里显示为什么需要权限的解释
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 直接请求权限
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13以下不需要单独请求通知权限
            startForegroundService()
        }
    }
    
    private fun startForegroundService() {
        ForegroundService.startService(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 不要在这里停止服务，因为我们希望服务在应用关闭后继续运行
        // ForegroundService.stopService(this)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AlarmInfoScreen(
    modifier: Modifier = Modifier,
    initialAlarmInfo: String,
    vibrationManager: VibrationManager? = null
) {
    val context = LocalContext.current
    var alarmInfo by remember { mutableStateOf(initialAlarmInfo) }
    
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 显示闹铃信息的文本
        Text(
            text = alarmInfo,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 刷新闹铃信息的按钮
        Button(
            onClick = {
                val alarmInfoManager = AlarmInfoManager(context)
                alarmInfo = alarmInfoManager.getNextAlarmTime()
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("刷新闹铃信息")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "振动模式测试",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 振动按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1小时振动按钮
            Button(
                onClick = {
                    vibrationManager?.vibrateStrong() ?: run {
                        val vm = VibrationManager(context)
                        vm.vibrateStrong()
                    }
                }
            ) {
                Text("1小时")
            }
            
            // 15分钟振动按钮
            Button(
                onClick = {
                    vibrationManager?.vibrateMedium() ?: run {
                        val vm = VibrationManager(context)
                        vm.vibrateMedium()
                    }
                }
            ) {
                Text("15分钟")
            }
            
            // 1分钟振动按钮
            Button(
                onClick = {
                    vibrationManager?.vibrateLight() ?: run {
                        val vm = VibrationManager(context)
                        vm.vibrateLight()
                    }
                }
            ) {
                Text("1分钟")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun AlarmInfoScreenPreview() {
    AlarmVibratorTheme {
        AlarmInfoScreen(
            initialAlarmInfo = "下一个闹铃时间: 2023-05-20 08:00:00"
        )
    }
}