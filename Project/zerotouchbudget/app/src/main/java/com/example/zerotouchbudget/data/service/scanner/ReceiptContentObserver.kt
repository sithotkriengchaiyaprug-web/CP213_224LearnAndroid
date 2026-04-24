package com.example.zerotouchbudget.data.service.scanner

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class ReceiptContentObserver(
    handler: Handler,
    private val onImageAdded: () -> Unit
) : ContentObserver(handler) {
    
    private var lastTriggerTime = 0L

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        
        // Debounce 3 seconds
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime > 3000) {
            lastTriggerTime = now
            onImageAdded()
        }
    }
}
