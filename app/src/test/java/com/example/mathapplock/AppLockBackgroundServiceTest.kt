package com.example.mathapplock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AppLockBackgroundServiceTest {

    @Test
    fun testExemptionWindow_onWeekdays_duringWorkingHours_returnsTrue() {
        // Monday 09:00:00 (Start boundary)
        val mondayStart = LocalDateTime.of(2026, 9, 14, 9, 0, 0)
        assertTrue(AppLockBackgroundService.isWithinExemptionWindow(mondayStart))

        // Wednesday 12:30:00 (Midday)
        val wednesdayMid = LocalDateTime.of(2026, 9, 16, 12, 30, 0)
        assertTrue(AppLockBackgroundService.isWithinExemptionWindow(wednesdayMid))

        // Friday 16:00:00 (End boundary)
        val fridayEnd = LocalDateTime.of(2026, 9, 18, 16, 0, 0)
        assertTrue(AppLockBackgroundService.isWithinExemptionWindow(fridayEnd))
    }

    @Test
    fun testExemptionWindow_onWeekdays_outsideWorkingHours_returnsFalse() {
        // Monday 08:59:59 (Right before start)
        val mondayBeforeStart = LocalDateTime.of(2026, 9, 14, 8, 59, 59)
        assertFalse(AppLockBackgroundService.isWithinExemptionWindow(mondayBeforeStart))

        // Friday 16:00:01 (Right after end)
        val fridayAfterEnd = LocalDateTime.of(2026, 9, 18, 16, 0, 1)
        assertFalse(AppLockBackgroundService.isWithinExemptionWindow(fridayAfterEnd))

        // Tuesday 23:00:00 (Night)
        val tuesdayNight = LocalDateTime.of(2026, 9, 15, 23, 0, 0)
        assertFalse(AppLockBackgroundService.isWithinExemptionWindow(tuesdayNight))
    }

    @Test
    fun testExemptionWindow_onWeekends_returnsFalse() {
        // Saturday 12:00:00 (Even during 9 AM - 4 PM)
        val saturdayNoon = LocalDateTime.of(2026, 9, 19, 12, 0, 0)
        assertFalse(AppLockBackgroundService.isWithinExemptionWindow(saturdayNoon))

        // Sunday 10:00:00 (Even during 9 AM - 4 PM)
        val sundayMorning = LocalDateTime.of(2026, 9, 20, 10, 0, 0)
        assertFalse(AppLockBackgroundService.isWithinExemptionWindow(sundayMorning))
    }
}
