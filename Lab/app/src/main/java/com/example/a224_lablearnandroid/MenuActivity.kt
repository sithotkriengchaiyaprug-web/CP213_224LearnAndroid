package com.example.a224_lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column(modifier = Modifier.fillMaxSize().padding(top = 50.dp, start = 16.dp, end = 16.dp)) {

                MenuButton("RPG Card Game", RPGCardActivity::class.java)
                MenuButton("Pokedex API", PokedexActivity::class.java)

                MenuButton("Shared Preferences", SharedPreferencesActivity::class.java)

            }
        }
    }

    // Helper Function สำหรับสร้างปุ่ม
    @Composable
    private fun <T> MenuButton(title: String, target: Class<T>) {
        Button(
            onClick = { startActivity(Intent(this, target)) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(title)
        }
    }
}