package com.example.a224_lablearnandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class RPGCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Lifecycle", "RPGCardActivity : onCreate")
        setContent {
            RPGCardView(
                onProfileClick = {
//                    startActivity(Intent(this@RPGCardActivity, LifeCycleComposeActivity::class.java))
                }
            )
        }
    }

    override fun onStart() { super.onStart(); Log.i("Lifecycle", "RPGCardActivity : onStart") }
    override fun onResume() { super.onResume(); Log.i("Lifecycle", "RPGCardActivity : onResume") }
    override fun onPause() { super.onPause(); Log.i("Lifecycle", "RPGCardActivity : onPause") }
    override fun onStop() { super.onStop(); Log.i("Lifecycle", "RPGCardActivity : onStop") }
    override fun onDestroy() { super.onDestroy(); Log.i("Lifecycle", "RPGCardActivity : onDestroy") }
    override fun onRestart() { super.onRestart(); Log.i("Lifecycle", "RPGCardActivity : onRestart") }
}

@Composable
fun RPGCardView(onProfileClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().background(color = Color.White).padding(32.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(32.dp).background(color = Color.LightGray)
        ) {
            Text(
                text = "HP",
                modifier = Modifier.align(alignment = Alignment.CenterStart).fillMaxWidth(fraction = 0.20f).background(color = Color.Green).padding(8.dp)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "Profile",
            modifier = Modifier.size(300.dp).align(Alignment.CenterHorizontally).padding(top = 16.dp).clickable { onProfileClick() }
        )

        var str by remember { mutableStateOf(10) }
        var agi by remember { mutableStateOf(10) }
        var int by remember { mutableStateOf(10) }
        var cat by remember { mutableStateOf(100) }

        Row(
            modifier = Modifier.fillMaxWidth().background(color = Color.LightGray).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatControl(name = "Str", value = str, onIncrease = { str++ }, onDecrease = { str-- })
            StatControl(name = "Agi", value = agi, onIncrease = { agi++ }, onDecrease = { agi-- })
            StatControl(name = "Int", value = int, onIncrease = { int++ }, onDecrease = { int-- })
            StatControl(name = "Cat", value = cat, onIncrease = { cat++ }, onDecrease = { cat-- })
        }
    }
}

@Composable
fun StatControl(name: String, value: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onIncrease) {
            Image(painter = painterResource(R.drawable.baseline_arrow_drop_up_24), contentDescription = "up", modifier = Modifier.size(30.dp))
        }
        Text(text = name, fontSize = 28.sp)
        Text(text = value.toString(), fontSize = 28.sp)
        Button(onClick = onDecrease) {
            Image(painter = painterResource(R.drawable.outline_arrow_drop_down_24), contentDescription = "down", modifier = Modifier.size(30.dp))
        }
    }
}

@Preview
@Composable
fun PreviewScreen() { RPGCardView() }