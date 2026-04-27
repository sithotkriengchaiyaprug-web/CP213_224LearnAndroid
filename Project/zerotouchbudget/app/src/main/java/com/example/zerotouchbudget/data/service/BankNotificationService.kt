package com.example.zerotouchbudget.data.service

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.zerotouchbudget.data.service.scanner.ReceiptContentObserver
import com.example.zerotouchbudget.data.service.scanner.SmartReceiptScanner
import com.example.zerotouchbudget.domain.usecase.ParseNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankNotificationService : NotificationListenerService() {

    @Inject
    lateinit var parseNotificationUseCase: ParseNotificationUseCase
    
    @Inject
    lateinit var smartScanner: SmartReceiptScanner

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var contentObserver: ReceiptContentObserver? = null

    private val targetBanks = listOf(
        "com.kasikorn.retail.mbanking.mac",
        "com.scb.phone",
        "com.bbl.mobilebanking",
        "th.co.krungthai.next"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("BankNotificationService", "Listener connected, registering ContentObserver")
        // Start Realtime Scan Observer
        contentObserver = ReceiptContentObserver(Handler(Looper.getMainLooper())) {
            Log.d("BankNotificationService", "New image detected by ContentObserver! Starting scan...")
            serviceScope.launch {
                // Scan images added in the last 5 minutes (limit 5 according to flow)
                smartScanner.scan(limit = 5, sinceTimestamp = System.currentTimeMillis() - 5 * 60 * 1000, isAutoScan = true)
            }
        }
        
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in targetBanks) return

        val extras = sbn.notification.extras
        val text = extras.getString(Notification.EXTRA_TEXT) ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

        serviceScope.launch {
            try {
                parseNotificationUseCase(title, text, sbn.packageName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        serviceScope.cancel()
    }
}