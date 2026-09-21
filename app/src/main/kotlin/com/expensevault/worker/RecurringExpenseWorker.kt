package com.expensevault.worker

import android.content.Context
import androidx.work.*
import com.expensevault.core.domain.usecase.ProcessRecurringRulesUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class RecurringExpenseWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val processRecurringRulesUseCase: ProcessRecurringRulesUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            processRecurringRulesUseCase()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "recurring_expense_evaluator"

        fun enqueuePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<RecurringExpenseWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
