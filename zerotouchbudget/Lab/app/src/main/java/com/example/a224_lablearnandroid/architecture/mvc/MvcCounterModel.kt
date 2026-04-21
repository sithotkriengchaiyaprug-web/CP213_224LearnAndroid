package com.example.a224_lablearnandroid.architecture.mvc

class MvcCounterModel {
    private var count = 0

    fun getCount(): Int {
        return count
    }

    fun incrementCounter() {
        count++
    }
}