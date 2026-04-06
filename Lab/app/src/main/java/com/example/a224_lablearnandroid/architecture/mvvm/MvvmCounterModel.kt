package com.example.a224_lablearnandroid.architecture.mvvm

class MvvmCounterModel {
    private var count = 0

    fun getCount(): Int {
        return count
    }

    fun incrementCounter() {
        count++
    }

    fun DecrementCounter() {
        count--
    }
}