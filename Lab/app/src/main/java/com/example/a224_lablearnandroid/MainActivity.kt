package com.example.a224_lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Gray)
                    .padding(32.dp)
            ) {
                // ส่วนของ HP Bar (พื้นหลังสีขาว)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(color = Color.White)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.24f)
                            .fillMaxSize()
                            .background(color = Color.Red)
                    )


                    Text(
                        text = "HP: 24/100",
                        modifier = Modifier
                            .align(alignment = Alignment.Center)
                            .padding(horizontal = 8.dp),
                        color = Color.Black
                    )
                }
            }
        }
    }
}