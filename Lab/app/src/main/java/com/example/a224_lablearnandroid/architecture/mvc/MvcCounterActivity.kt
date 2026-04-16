package com.example.a224_lablearnandroid.architecture.mvc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.a224_lablearnandroid.R

class MvcCounterActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var btnIncrement: Button
    
    // Model
    private val model = MvcCounterModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counter_classic)

        // View: Initialize UI references
        tvCount = findViewById(R.id.tvCount)
        btnIncrement = findViewById(R.id.btnIncrement)

        // Controller Logic: Handle user input (click)
        btnIncrement.setOnClickListener {
            // 1. Notify Model to update logic
            model.incrementCounter()
            
            // 2. Get new data from Model
            val newCount = model.getCount()
            
            // 3. Update View
            updateView(newCount)
        }
    }

    private fun updateView(count: Int) {
        tvCount.text = "Count: $count"
    }
}