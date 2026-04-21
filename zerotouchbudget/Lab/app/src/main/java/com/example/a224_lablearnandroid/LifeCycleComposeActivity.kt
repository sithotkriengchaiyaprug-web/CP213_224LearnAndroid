package com.example.a224_lablearnandroid

import android.os.Bundle
import android.util.Log // à¸•à¹‰à¸­à¸‡à¸¡à¸µà¸•à¸±à¸§à¸™à¸µà¹‰à¹€à¸žà¸·à¹ˆà¸­à¹ƒà¸Šà¹‰ Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// à¸•à¹‰à¸­à¸‡à¸¡à¸µ 2 à¸•à¸±à¸§à¸™à¸µà¹‰à¹€à¸žà¸·à¹ˆà¸­à¹ƒà¸«à¹‰ 'by' à¹„à¸¡à¹ˆà¹à¸”à¸‡
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.a224_lablearnandroid.ui.theme._224LearnAndroidTheme

class MainActivity2 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _224LearnAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting2(
                        name = "Sithichot",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        Log.i("Lifecycle", "MainActivity : onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.i("Lifecycle", "MainActivity : onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("Lifecycle", "MainActivity : onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("Lifecycle", "MainActivity : onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("Lifecycle", "MainActivity : onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Lifecycle", "MainActivity : onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("Lifecycle", "MainActivity : onRestart")
    }
}

@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Text(
            text = "Hello $name!"
        )
        TextField(
            value = inputText,
            onValueChange = {
                inputText = it
            },
            label = { Text("à¸à¸£à¸­à¸à¸‚à¹‰à¸­à¸„à¸§à¸²à¸¡à¸—à¸µà¹ˆà¸™à¸µà¹ˆ") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    _224LearnAndroidTheme {
        Greeting2("Android")
    }
}