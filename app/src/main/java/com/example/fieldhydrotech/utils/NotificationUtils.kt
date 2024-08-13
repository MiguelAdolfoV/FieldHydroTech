package com.example.fieldhydrotech.utils

import android.view.View
import android.widget.TextView

class NotificationUtils {

    var notificationCount = 0

    fun updateNotificationCount(badge: TextView) {
        // Incrementar el contador de notificaciones
        notificationCount++

        // Actualizar la visualización del badge
        if (notificationCount > 99) {
            badge.text = "99+"
        } else {
            badge.text = notificationCount.toString()
        }

        badge.visibility = if (notificationCount > 0) View.VISIBLE else View.GONE
    }
}
