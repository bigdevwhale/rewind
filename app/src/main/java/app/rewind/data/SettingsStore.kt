package app.rewind.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.rewind.domain.Hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** The store keeps evening hours; off, it never closes. */
    val hoursOn: Boolean = true,
    /** Opening hour on weekdays, and on Saturdays and Sundays. */
    val weekdayOpen: Int = 18,
    val weekendOpen: Int = 12,
    /** Closing hour; at or before the opening hour it means past midnight. */
    val closeAt: Int = 23,
    /** Days a new tape spends in the window before it reaches the shelf; 0 puts it straight there. */
    val windowDays: Int = 3,
    /** A nudge before a tape is due, and when one leaves the window. */
    val reminders: Boolean = true,
    /** The name signed on the cards of tapes recommended to friends. */
    val name: String = "",
) {
    val hours: Hours get() = Hours(hoursOn, weekdayOpen, weekendOpen, closeAt)
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        hoursOn = this[HOURS_ON] ?: true,
        weekdayOpen = this[WEEKDAY_OPEN] ?: 18,
        weekendOpen = this[WEEKEND_OPEN] ?: 12,
        closeAt = this[CLOSE_AT] ?: 23,
        windowDays = this[WINDOW_DAYS] ?: 3,
        reminders = this[REMINDERS] ?: true,
        name = this[NAME] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[HOURS_ON] = s.hoursOn
            it[WEEKDAY_OPEN] = s.weekdayOpen
            it[WEEKEND_OPEN] = s.weekendOpen
            it[CLOSE_AT] = s.closeAt
            it[WINDOW_DAYS] = s.windowDays
            it[REMINDERS] = s.reminders
            it[NAME] = s.name
        }
    }

    /** Until when reminders have been sent, so none goes out twice. */
    suspend fun remindedUntil(): Long = store.data.first()[REMINDED] ?: 0L

    suspend fun reminded(at: Long) {
        store.edit { it[REMINDED] = at }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val HOURS_ON = booleanPreferencesKey("hours_on")
        val WEEKDAY_OPEN = intPreferencesKey("weekday_open")
        val WEEKEND_OPEN = intPreferencesKey("weekend_open")
        val CLOSE_AT = intPreferencesKey("close_at")
        val WINDOW_DAYS = intPreferencesKey("window_days")
        val REMINDERS = booleanPreferencesKey("reminders")
        val NAME = stringPreferencesKey("name")
        val REMINDED = longPreferencesKey("reminded")
    }
}
