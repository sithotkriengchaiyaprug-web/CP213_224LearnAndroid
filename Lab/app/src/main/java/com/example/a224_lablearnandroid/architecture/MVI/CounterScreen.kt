package com.example.a224_lablearnandroid.architecture.mvi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun CounterScreen(counterViewModel: CounterViewModel) {

    // 1. à¸ªà¸±à¸‡à¹€à¸à¸•à¸à¸²à¸£à¸“à¹Œ (Observe) State à¸ˆà¸²à¸ ViewModel
    //    à¹€à¸¡à¸·à¹ˆà¸­ State à¹ƒà¸™ ViewModel à¹€à¸›à¸¥à¸µà¹ˆà¸¢à¸™, UI à¸ªà¹ˆà¸§à¸™à¸™à¸µà¹‰à¸ˆà¸° Recompose (à¸§à¸²à¸”à¹ƒà¸«à¸¡à¹ˆ) à¸­à¸±à¸•à¹‚à¸™à¸¡à¸±à¸•à¸´
    val state by counterViewModel.state.collectAsState()


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 2. à¹à¸ªà¸”à¸‡à¸œà¸¥à¸‚à¹‰à¸­à¸¡à¸¹à¸¥à¸ˆà¸²à¸ State
        Text(
            text = "Count: ${state.count}",
            fontSize = 32.sp
        )

        // 3. à¹€à¸¡à¸·à¹ˆà¸­à¸¡à¸µà¸à¸²à¸£à¸à¸”à¸›à¸¸à¹ˆà¸¡, à¸ªà¹ˆà¸‡ Intent à¹€à¸‚à¹‰à¸²à¹„à¸›à¹ƒà¸™à¸Ÿà¸±à¸‡à¸à¹Œà¸Šà¸±à¸™ onIntent
        Button(onClick = {
            counterViewModel.processIntent(CounterIntent.IncrementCounter)
        }) {
            Text("Add +1")
        }

        Button(onClick = {
            counterViewModel.processIntent(CounterIntent.DecrementCounter)
        }) {
            Text("remove -1")
        }
    }



//    // à¹€à¸£à¸µà¸¢à¸à¹ƒà¸Šà¹‰à¸Ÿà¸±à¸‡à¸à¹Œà¸Šà¸±à¸™à¸ªà¸³à¸«à¸£à¸±à¸šà¹à¸ªà¸”à¸‡à¸œà¸¥ UI à¹‚à¸”à¸¢à¸ªà¹ˆà¸‡ state à¹à¸¥à¸° lambda à¸ªà¸³à¸«à¸£à¸±à¸šà¸ªà¹ˆà¸‡ intent à¹€à¸‚à¹‰à¸²à¹„à¸›
//    CounterView(state = state, onIntent = { intent ->
//        counterViewModel.processIntent(intent)
//    })


}

@Composable
fun CounterView(state: CounterState, onIntent: (CounterIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 2. à¹à¸ªà¸”à¸‡à¸œà¸¥à¸‚à¹‰à¸­à¸¡à¸¹à¸¥à¸ˆà¸²à¸ State
        Text(
            text = "Count: ${state.count}",
            fontSize = 32.sp
        )

        // 3. à¹€à¸¡à¸·à¹ˆà¸­à¸¡à¸µà¸à¸²à¸£à¸à¸”à¸›à¸¸à¹ˆà¸¡, à¸ªà¹ˆà¸‡ Intent à¹€à¸‚à¹‰à¸²à¹„à¸›à¹ƒà¸™à¸Ÿà¸±à¸‡à¸à¹Œà¸Šà¸±à¸™ onIntent
        Button(onClick = {
            onIntent(CounterIntent.IncrementCounter)
        }) {
            Text("Add +1")
        }
    }
}