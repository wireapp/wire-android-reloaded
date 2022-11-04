package com.wire.android.migration

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.feature.MigrateConversationsUseCase
import com.wire.android.migration.feature.MigrateMessagesUseCase
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.failure.ServerConfigFailure
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.mapLeft
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore,
    private val migrateServerConfig: MigrateServerConfigUseCase,
    private val migrateActiveAccounts: MigrateActiveAccountsUseCase,
    private val migrateClientsData: MigrateClientsDataUseCase,
    private val migrateConversations: MigrateConversationsUseCase,
    private val migrateMessages: MigrateMessagesUseCase,
) {

    private fun isScalaDBPresent(): Boolean =
        applicationContext.getDatabasePath(ScalaDBNameProvider.globalDB()).let { it.isFile && it.exists() }

    suspend fun shouldMigrate(): Boolean = when {
        // already migrated
        globalDataStore.isMigrationCompleted() -> false
        // not yet migrated and old DB is present
        isScalaDBPresent() -> true
        // not yet migrated and no DB to migrate from - skip and set as migrated because it's not an update of the old app version
        else -> globalDataStore.setMigrationCompleted().let { false }
    }

    fun isMigrationCompletedFlow(): Flow<Boolean> = globalDataStore.isMigrationCompletedFlow()

    suspend fun migrate(): MigrationResult =
        migrateServerConfig()
            .flatMap { migrateActiveAccounts(it) }
            .flatMap { migrateClientsData(it) }
            .flatMap { migrateConversations(it) }
            .flatMap { migrateMessages(it) }
            .mapLeft(::migrationFailure)
            .onSuccess { globalDataStore.setMigrationCompleted() }
            .fold({ MigrationResult.Failure(it) }, { MigrationResult.Success })

    private fun migrationFailure(failure: CoreFailure): MigrationResult.Failure.Type = when (failure) {
        is NetworkFailure.NoNetworkConnection -> MigrationResult.Failure.Type.NO_NETWORK
        is StorageFailure.DataNotFound -> MigrationResult.Failure.Type.DATA_NOT_FOUND
        is ServerConfigFailure.UnknownServerVersion -> MigrationResult.Failure.Type.UNKNOWN_SERVER_VERSION
        is ServerConfigFailure.NewServerVersion -> MigrationResult.Failure.Type.TOO_NEW_VERSION
        is MigrationFailure.InvalidRefreshToken -> MigrationResult.Failure.Type.UNKNOWN
        else -> MigrationResult.Failure.Type.UNKNOWN
    }
}

sealed class MigrationResult {
    object Success : MigrationResult()
    data class Failure(val type: Type) : MigrationResult() {
        enum class Type { UNKNOWN_SERVER_VERSION, TOO_NEW_VERSION, DATA_NOT_FOUND, NO_NETWORK, UNKNOWN; }
        companion object {
            const val KEY_FAILURE_TYPE = "failure_type"
        }
    }
}

fun MigrationResult.Failure.Type.toData(): Data = workDataOf(MigrationResult.Failure.KEY_FAILURE_TYPE to this.name)

fun Data.getMigrationFailure(): MigrationResult.Failure.Type = this.getString(MigrationResult.Failure.KEY_FAILURE_TYPE)
    ?.let {
        try {
            MigrationResult.Failure.Type.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: MigrationResult.Failure.Type.UNKNOWN
