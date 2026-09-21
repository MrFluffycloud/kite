package com.expensevault.worker

import android.content.Context
import androidx.work.*
import com.expensevault.core.domain.repository.ExchangeRateRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class ExchangeRateSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val exchangeRateRepository: ExchangeRateRepository by inject()

    override suspend fun doWork(): Result {
        // TODO: Get base currency from preferences
        val baseCurrency = "INR"
        return when {
            exchangeRateRepository.fetchAndCacheRates(baseCurrency).isSuccess -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "exchange_rate_sync"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(
                24, TimeUnit.HOURS
            ).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
