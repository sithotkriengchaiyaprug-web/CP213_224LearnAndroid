package com.example.a224_lablearnandroid.architecture.mvp

class MvpCounterModel {
    private var count = 0

    fun getCount(): Int {
        return count
    }

    fun incrementCounter() {
        count++
    }
}