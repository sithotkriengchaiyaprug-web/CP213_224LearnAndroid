package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.a224_lablearnandroid.ui.theme._224LearnAndroidTheme
import com.example.a224_lablearnandroid.utils.SharedPreferencesUtil

class SharedPreferencesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedPreferencesUtil.init(this)
        // à¸à¸²à¸£à¸šà¸±à¸™à¸—à¸¶à¸à¸„à¹ˆà¸² (à¹€à¸Šà¹ˆà¸™ à¹€à¸¡à¸·à¹ˆà¸­à¸à¸”à¸›à¸¸à¹ˆà¸¡ Save)
        SharedPreferencesUtil.saveString("user_name", "Sithichot")
        SharedPreferencesUtil.saveBoolean("is_dark_mode", true)

        // à¸à¸²à¸£à¸”à¸¶à¸‡à¸„à¹ˆà¸²à¸¡à¸²à¹ƒà¸Šà¹‰à¸‡à¸²à¸™ (à¹€à¸Šà¹ˆà¸™ à¹€à¸¡à¸·à¹ˆà¸­à¹€à¸›à¸´à¸”à¹à¸­à¸žà¸‚à¸¶à¹‰à¸™à¸¡à¸²à¹ƒà¸«à¸¡à¹ˆ)
        val name = SharedPreferencesUtil.getString("user_name")
        val darkMode = SharedPreferencesUtil.getBoolean("is_dark_mode")

        enableEdgeToEdge()
        setContent {
            _224LearnAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting3(
                        name = name,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting3(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview3() {
    _224LearnAndroidTheme {
        Greeting3("Android")
    }
}