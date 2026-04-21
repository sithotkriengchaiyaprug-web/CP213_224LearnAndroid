package com.example.a224_lablearnandroid.architecture.mvvm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MvvmCounterViewModel : ViewModel() {

    private val model = MvvmCounterModel()

    // StateFlow for View to Observe
    private val _count = MutableStateFlow(model.getCount()) // 11
    val count: StateFlow<Int> = _count.asStateFlow()

    fun onIncrementClicked() {
        model.incrementCounter()
        _count.value = model.getCount()
    }

    fun onDecrementClicked() {
        model.DecrementCounter()
        _count.value = model.getCount()
    }
}