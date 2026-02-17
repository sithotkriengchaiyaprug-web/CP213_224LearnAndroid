package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class SharedPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedPreferencesUtil.init(this)
        SharedPreferencesUtil.saveString("user_name", "นักศึกษา 224")

        val name = SharedPreferencesUtil.getString("user_name") ?: "Guest"

        setContent {
            Text(text = "ยินดีต้อนรับ: $name", modifier = Modifier.padding(20.dp))
        }
    }
}