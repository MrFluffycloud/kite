package com.expensevault.core.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object VaultDatabaseFactory {
    fun create(context: Context, passphraseBlob: ByteArray): VaultDatabase {
        val factory = SupportOpenHelperFactory(passphraseBlob)
        return Room.databaseBuilder(
            context.applicationContext,
            VaultDatabase::class.java,
            "vault_data.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }
}
