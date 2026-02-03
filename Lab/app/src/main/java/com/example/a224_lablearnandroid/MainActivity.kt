package com.example.a224_lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
    @Composable
    fun MainScreen(){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Gray)
                .padding(16.dp)
        ) {
            // HP Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(color = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.24f)
                        .fillMaxHeight()
                        .background(color = Color.Red)
                )
                Text(text = "HP: 24/100", modifier = Modifier.align(Alignment.Center))
            }

            // Profile Image
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "profile",
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
                    .clickable {
                        startActivity(Intent(this@MainActivity, ListActivity::class.java))
                    }

            )
            var str by remember { mutableStateOf(8) }
            var agi by remember { mutableStateOf(10) }
            var int by remember { mutableStateOf(15) }
            // Status Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly // จัดระยะห่างให้สวยงาม
            ) {
                // Column สำหรับ Str
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { str++ }) {
                        Image(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_up_24),
                            contentDescription = "up",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(text = "Str", fontSize = 24.sp)
                    Text(text = str.toString(), fontSize = 32.sp, color = Color.White)
                    Button(onClick = { if (str > 0) str-- }) {
                        Image(
                            painter = painterResource(id = R.drawable.outline_arrow_drop_down_24),
                            contentDescription = "down",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Column สำหรับ Agi
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { agi++ }) {
                        Image(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_up_24),
                            contentDescription = "up",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(text = "Agi", fontSize = 24.sp)
                    Text(text = agi.toString(), fontSize = 32.sp, color = Color.White)
                    Button(onClick = { if (agi > 0) agi-- }) {
                        Image(
                            painter = painterResource(id = R.drawable.outline_arrow_drop_down_24),
                            contentDescription = "down",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Column สำหรับ Int
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { int++ }) {
                        Image(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_up_24),
                            contentDescription = "up",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(text = "Int", fontSize = 24.sp)
                    Text(text = int.toString(), fontSize = 32.sp, color = Color.White)
                    Button(onClick = { if (int > 0) int-- }) {
                        Image(
                            painter = painterResource(id = R.drawable.outline_arrow_drop_down_24),
                            contentDescription = "down",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            } // ปิด Row
        } // ปิด Column หลัก
    }

    @Preview
    @Composable
    fun previesScreen(){
        MainScreen()
    }
} // ปิด class


