package com.example.zerotouchbudget.data.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.glance.appwidget.updateAll
import com.example.zerotouchbudget.domain.usecase.ParseNotificationUseCase
import com.example.zerotouchbudget.presentation.widget.BudgetWidget
import dagger.hilt.android.AndroidEntryPoint
import androidx.glance.appwidget.updateAll
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
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

        serviceScope.launch {
            try {
<<<<<<< ours
                parseNotificationUseCase(
                    title = title,
                    text = text,
                    packageName = sbn.packageName,
                    notificationKey = sbn.key
                )
                BudgetWidget().updateAll(this@BankNotificationService)
=======
                parseNotificationUseCase(title, text, sbn.packageName)
                BudgetWidget().updateAll(applicationContext)
>>>>>>> theirs
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
