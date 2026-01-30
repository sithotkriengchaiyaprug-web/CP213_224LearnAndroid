package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.a224_lablearnandroid.ui.theme._224_LabLearnAndroidTheme

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _224_LabLearnAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting2(
                        name = "Android2",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Text(
            text = "Hello $name! say = $inputText"
        )
        TextField(
            value = inputText, // ต้องเป็นตัว v พิมพ์เล็ก (ไม่ใช่ Value)
            onValueChange = { newValue ->
                inputText = newValue // อัปเดตค่าเมื่อพิมพ์
            },
            label = { Text("กรอกข้อความที่นี่") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    _224_LabLearnAndroidTheme {
        Greeting2("Android")
    }
}