package com.brogaming.trackmyauto.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.brogaming.trackmyauto.data.storage.AppPrefs

@Composable
fun SettingsScreen(context:Context){
    AutoStartToggle(context)
}

@Composable
fun AutoStartToggle(context: Context) {
    var enabled by remember { mutableStateOf(AppPrefs.isAutoStartOnBoot(context)) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Auto Start Tracking After Reboot")
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                AppPrefs.setAutoStartOnBoot(context, it)
            }
        )
    }
}
