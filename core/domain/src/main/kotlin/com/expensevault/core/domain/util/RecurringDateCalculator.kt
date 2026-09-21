package com.expensevault.core.domain.util

import com.expensevault.core.model.RecurringFrequency
import com.expensevault.core.model.RecurringRule
import kotlinx.datetime.LocalDate

object RecurringDateCalculator {

    fun calculateNextOccurrence(rule: RecurringRule, fromDate: LocalDate = rule.nextOccurrence): LocalDate {
        return when (rule.frequency) {
            RecurringFrequency.WEEKLY -> {
                LocalDate.fromEpochDays(fromDate.toEpochDays() + 7)
            }
            RecurringFrequency.MONTHLY -> {
                val currentYear = fromDate.year
                val currentMonth = fromDate.monthNumber
                val targetDay = rule.dayOfMonth ?: fromDate.dayOfMonth

                val (nextYear, nextMonth) = if (currentMonth == 12) {
                    Pair(currentYear + 1, 1)
                } else {
                    Pair(currentYear, currentMonth + 1)
                }

                val maxDays = daysInMonth(nextYear, nextMonth)
                val day = minOf(targetDay, maxDays)
                LocalDate(nextYear, nextMonth, day)
            }
            RecurringFrequency.CUSTOM -> {
                val interval = rule.customIntervalDays ?: 30
                LocalDate.fromEpochDays(fromDate.toEpochDays() + interval)
            }
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
