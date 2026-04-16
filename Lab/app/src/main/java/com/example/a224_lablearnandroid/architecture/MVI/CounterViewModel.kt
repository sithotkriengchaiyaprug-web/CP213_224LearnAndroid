package com.example.a224_lablearnandroid.architecture.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel: à¸•à¸±à¸§à¸à¸¥à¸²à¸‡à¸—à¸µà¹ˆà¸£à¸±à¸š Intent à¹à¸¥à¸°à¸ªà¸£à¹‰à¸²à¸‡ State à¹ƒà¸«à¸¡à¹ˆ
 */
class CounterViewModel : ViewModel() {

    // à¸ªà¸£à¹‰à¸²à¸‡ StateFlow à¸ªà¸³à¸«à¸£à¸±à¸šà¹€à¸à¹‡à¸š State à¸›à¸±à¸ˆà¸ˆà¸¸à¸šà¸±à¸™ (private, à¹à¸à¹‰à¹„à¸‚à¹„à¸”à¹‰à¸ à¸²à¸¢à¹ƒà¸™)
    private val _state = MutableStateFlow(CounterState())

    // à¹€à¸›à¸´à¸” StateFlow à¹ƒà¸«à¹‰à¸­à¹ˆà¸²à¸™à¹„à¸”à¹‰à¸­à¸¢à¹ˆà¸²à¸‡à¹€à¸”à¸µà¸¢à¸§ (public) à¹€à¸žà¸·à¹ˆà¸­à¹ƒà¸«à¹‰ View à¸™à¸³à¹„à¸› Observe
    val state: StateFlow<CounterState> = _state

    /**
     * à¸Ÿà¸±à¸‡à¸à¹Œà¸Šà¸±à¸™à¸«à¸¥à¸±à¸à¸ªà¸³à¸«à¸£à¸±à¸šà¸£à¸±à¸š Intent à¸ˆà¸²à¸ View
     */
    fun processIntent(intent: CounterIntent) {
        viewModelScope.launch {
            when (intent) {
                is CounterIntent.IncrementCounter -> {
                    incrementCounter()
                }
                is CounterIntent.DecrementCounter -> {
                    DecrementCounter()
                }
            }
        }
    }

    /**
     * Logic à¸à¸²à¸£à¸šà¸§à¸à¹€à¸¥à¸‚
     * à¸ªà¸£à¹‰à¸²à¸‡ State à¹ƒà¸«à¸¡à¹ˆà¹‚à¸”à¸¢à¸à¸²à¸£ copy à¸‚à¸­à¸‡à¹€à¸à¹ˆà¸²à¹à¸¥à¹‰à¸§à¸šà¸§à¸à¸„à¹ˆà¸²à¹€à¸žà¸´à¹ˆà¸¡à¹€à¸‚à¹‰à¸²à¹„à¸›
     */
    private fun incrementCounter() {
        val currentState = _state.value
        _state.value = currentState.copy(count = currentState.count + 1)
    }
    private fun DecrementCounter() {
        val currentState = _state.value
        _state.value = currentState.copy(count = currentState.count - 1)
    }
}