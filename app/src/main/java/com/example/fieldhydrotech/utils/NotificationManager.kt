import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.fieldhydrotech.R

data class Notification(val iconResId: Int, val title: String)

class NotificationManager(private val context: Context, private val container: LinearLayout) {

    fun addNotification(notification: Notification) {
        // Inflar la vista de la notificación
        val inflater = LayoutInflater.from(context)
        val notificationView = inflater.inflate(R.layout.notification_item, container, false)

        // Configurar la vista de la notificación
        val icon = notificationView.findViewById<ImageView>(R.id.item_icon)
        val title = notificationView.findViewById<TextView>(R.id.item_title)

        icon.setImageResource(notification.iconResId)
        title.text = notification.title

        // Añadir la vista al contenedor
        container.addView(notificationView)

        // Agregar OnClickListener para eliminar la notificación cuando se haga clic en ella
        notificationView.setOnClickListener {
            // Remover la vista del contenedor
            container.removeView(notificationView)
        }
    }

    fun clearNotifications() {
        container.removeAllViews()
    }

    fun hasNotifications(): Boolean {
        return container.childCount > 0
    }

}
