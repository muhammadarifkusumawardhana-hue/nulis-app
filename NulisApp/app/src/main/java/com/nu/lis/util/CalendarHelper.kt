package com.nu.lis.util

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.nu.lis.data.TaskEntity
import java.util.TimeZone

object CalendarHelper {

    fun syncToCalendar(context: Context, task: TaskEntity): Long? {
        val dueDate = task.dueDate ?: return null
        
        val contentResolver = context.contentResolver

        // Find a writable calendar, preferably primary
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        
        val cursor = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        )
        
        var calendarId: Long? = null
        
        cursor?.use {
            val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
            val primaryIndex = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
            val accessIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            
            while (it.moveToNext()) {
                val accessLevel = it.getInt(accessIndex)
                // Check if writable (greater than or equal to CALENDAR_ACCESS_CONTRIBUTOR (500))
                if (accessLevel >= 500) {
                    val isPrimary = it.getInt(primaryIndex) == 1
                    if (isPrimary) {
                        calendarId = it.getLong(idIndex)
                        break
                    }
                    // Keep the first writable one as fallback
                    if (calendarId == null) {
                        calendarId = it.getLong(idIndex)
                    }
                }
            }
        }

        if (calendarId == null) {
            Log.e("CalendarHelper", "No writable calendar found")
            return null
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, dueDate)
            put(CalendarContract.Events.DTEND, dueDate + 3600000) // 1 hour duration
            put(CalendarContract.Events.TITLE, task.title.ifBlank { "Task Reminder" })
            put(CalendarContract.Events.DESCRIPTION, task.description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            if (task.calendarEventId != null) {
                // Update existing event
                val updateUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                    .appendPath(task.calendarEventId.toString())
                    .build()
                val rows = contentResolver.update(updateUri, values, null, null)
                if (rows > 0) {
                    task.calendarEventId
                } else {
                    // Event might have been deleted manually in Calendar app, re-insert
                    val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    val newId = uri?.lastPathSegment?.toLong()
                    if (newId != null) addReminder(contentResolver, newId)
                    newId
                }
            } else {
                // Insert new event
                val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                val eventId = uri?.lastPathSegment?.toLong()
                if (eventId != null) addReminder(contentResolver, eventId)
                eventId
            }
        } catch (e: Exception) {
            Log.e("CalendarHelper", "Error syncing to calendar", e)
            null
        }
    }

    private fun addReminder(contentResolver: android.content.ContentResolver, eventId: Long) {
        try {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.MINUTES, 10)
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        } catch (e: Exception) {
            Log.e("CalendarHelper", "Error adding reminder", e)
        }
    }

    fun removeFromCalendar(context: Context, eventId: Long) {
        try {
            val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendPath(eventId.toString())
                .build()
            context.contentResolver.delete(deleteUri, null, null)
        } catch (e: Exception) {
            Log.e("CalendarHelper", "Error removing from calendar", e)
        }
    }
}
