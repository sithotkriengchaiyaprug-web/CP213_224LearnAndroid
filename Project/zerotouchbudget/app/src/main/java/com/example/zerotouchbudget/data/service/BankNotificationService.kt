package com.example.zerotouchbudget.data.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val targetBanks = listOf(
        "com.kasikorn.retail.mbanking.mac",
        "com.scb.phone",
        "com.bbl.mobilebanking",
        "th.co.krungthai.next"
    )



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
        serviceScope.cancel()
    }
}