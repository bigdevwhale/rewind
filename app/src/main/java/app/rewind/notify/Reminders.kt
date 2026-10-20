package app.rewind.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.rewind.MainActivity
import app.rewind.R
import app.rewind.container
import app.rewind.data.Rental
import app.rewind.data.Tape
import app.rewind.domain.Rules
import app.rewind.localized
import app.rewind.ui.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The store's two nudges: a tape due back in a few hours (and once it's late), and a new tape leaving
 * the window for the shelf. One inexact alarm waits for the next of them; when it rings, everything
 * that fell due since the last ring is sent, and the next is set.
 */
object Reminders {
    private const val CHANNEL = "store"
    private const val SOON = 3 * Rules.HOUR

    /** A moment worth a nudge. */
    sealed class Event(val at: Long) {
        class DueSoon(val tape: Tape, val rental: Rental) : Event(rental.dueAt - SOON)
        class Overdue(val tape: Tape, val rental: Rental) : Event(rental.dueAt)
        class Shelved(val tape: Tape) : Event(tape.readyAt)
    }

    fun events(tapes: List<Tape>, rentals: List<Rental>): List<Event> {
        val byId = tapes.associateBy { it.id }
        val out = rentals.filter { it.out }.flatMap { r ->
            val t = byId[r.tapeId] ?: return@flatMap emptyList()
            // A rent shorter than the warning gets only the "late" one.
            if (r.dueAt - r.outAt > SOON * 2) listOf(Event.DueSoon(t, r), Event.Overdue(t, r)) else listOf(Event.Overdue(t, r))
        }
        val window = tapes.filter { it.readyAt > it.addedAt }.map { Event.Shelved(it) }
        return (out + window).sortedBy { it.at }
    }

    fun schedule(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch { arm(app, System.currentTimeMillis()) }
    }

    private suspend fun arm(context: Context, now: Long) {
        val c = context.container
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, Intent(context, Ring::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val dao = c.db.dao()
        val next = events(dao.tapes(), dao.rentals()).firstOrNull { it.at > now }
        if (next == null || !c.settings.now().reminders) {
            am.cancel(pi)
            return
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi)
    }

    /** Sends whatever fell due since the last ring, then waits for the next. */
    suspend fun ring(context: Context) {
        val c = context.container
        val now = System.currentTimeMillis()
        val since = c.settings.remindedUntil().takeIf { it > 0 } ?: (now - Rules.HOUR)
        c.settings.reminded(now)
        if (c.settings.now().reminders) {
            val dao = c.db.dao()
            events(dao.tapes(), dao.rentals()).filter { it.at in (since + 1)..now }.forEach { post(context, it) }
        }
        arm(context, now)
    }

    private fun post(context: Context, e: Event) {
        val l = context.localized()
        channel(context, l)
        val (title, text, id) = when (e) {
            is Event.DueSoon -> Triple(l.getString(R.string.n_due_title, e.tape.title), l.getString(R.string.n_due_text, T.time(l, e.rental.dueAt)), e.rental.id.toInt())
            is Event.Overdue -> Triple(l.getString(R.string.n_late_title, e.tape.title), l.getString(R.string.n_late_text), e.rental.id.toInt())
            is Event.Shelved -> Triple(l.getString(R.string.n_shelf_title, e.tape.title), l.getString(R.string.n_shelf_text), 100_000 + e.tape.id.toInt())
        }
        val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Notifications turned off: the store stays quiet.
        }
    }

    private fun channel(context: Context, l: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, l.getString(R.string.n_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }

    class Ring : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val done = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ring(context.applicationContext)
                } finally {
                    done.finish()
                }
            }
        }
    }

    /** After a restart, or an update, the alarm has to be set again. */
    class Boot : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) schedule(context)
        }
    }
}
