package tn.isetbizerte.tp4_alertes


import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent

object Alertes {

    const val EXTRA_ACTION = "action"
    const val EXTRA_TITRE = "titre"
    const val EXTRA_ID = "id"

    const val CANAL_ID = "alertes_critiques"

    // « Voir » : rouvre MainActivity avec les détails de l'alerte
    private fun intentVoir(context: Context, id: Int, titre: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, "voir")
            putExtra(EXTRA_TITRE, titre)
            putExtra(EXTRA_ID, id)
        }
        // requestCode = id : un PendingIntent distinct par alerte
        return PendingIntent.getActivity(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // « Ignorer » : réveille AlerteReceiver, sans ouvrir l'application
    private fun intentIgnorer(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, AlerteReceiver::class.java).putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun fermer(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)



    // À appeler avant toute notification (sans effet si le canal existe déjà)
    fun creerCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL_ID, "Alertes critiques", NotificationManager.IMPORTANCE_HIGH   // HIGH = bannière
        ).apply { description = "Alertes des capteurs nécessitant une action" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    // false si l'utilisateur a refusé ou coupé les notifications
    fun autorisee(context: Context) =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")      // vérifié par autorisee() avant chaque appel
    fun afficher(context: Context, titre: String, texte: String): Int {
        val id = (System.currentTimeMillis() % 100_000).toInt()    // un id différent par alerte
        val notification = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)        // obligatoire, sinon plantage
            .setContentTitle(titre)
            .setContentText(texte)
            .setPriority(NotificationCompat.PRIORITY_HIGH)            // Android 7 et moins
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

            .setContentIntent(intentVoir(context, id, titre))              // toucher le corps
            .addAction(0, "Voir", intentVoir(context, id, titre))
            .addAction(0, "Ignorer", intentIgnorer(context, id)) // disparaît quand on la touche
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        return id
    }
}

// Reçoit le clic sur « Ignorer », même application fermée
class AlerteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alertes.fermer(context, intent.getIntExtra(Alertes.EXTRA_ID, 0))
    }
}