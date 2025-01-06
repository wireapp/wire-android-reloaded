/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.di

import android.content.Context
import androidx.work.WorkManager
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.util.ImageUtil
import com.wire.android.util.UserAgentProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ObserveOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveTeamSettingsSelfDeletingStatusUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.feature.user.MarkFileSharingChangeAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.MarkSelfDeletionStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.network.NetworkStateObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KaliumCoreLogic

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentSessionFlowService

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrentAccount

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoSession

@Module
@InstallIn(SingletonComponent::class)
class CoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun provideCoreLogic(
        @ApplicationContext context: Context,
        kaliumConfigs: KaliumConfigs,
        userAgentProvider: UserAgentProvider
    ): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path

        return CoreLogic(
            userAgent = userAgentProvider.defaultUserAgent,
            appContext = context,
            rootPath = rootPath,
            kaliumConfigs = kaliumConfigs
        )
    }

    @Singleton
    @Provides
    fun provideNetworkStateObserver(@KaliumCoreLogic coreLogic: CoreLogic): NetworkStateObserver =
        coreLogic.networkStateObserver

    @Provides
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun deleteSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().deleteSession

    @Provides
    fun provideUpdateCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): UpdateCurrentSessionUseCase =
        coreLogic.getGlobalScope().session.updateCurrentSession

    @Provides
    fun provideGetAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideServerConfigForAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ServerConfigForAccountUseCase =
        coreLogic.getGlobalScope().serverConfigForAccounts

    @NoSession
    @Singleton
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapperImpl(null)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext applicationContext: Context) = WorkManager.getInstance(applicationContext)
}

@Module
@InstallIn(ViewModelComponent::class)
class SessionModule {
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    @CurrentAccount
    @ViewModelScoped
    @Provides
    fun provideCurrentSession(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            return@runBlocking when (val result = coreLogic.getGlobalScope().session.currentSession.invoke()) {
                is CurrentSessionResult.Success -> result.accountInfo.userId
                else -> {
                    throw IllegalStateException("no current session was found")
                }
            }
        }
    }

    @ViewModelScoped
    @Provides
    fun provideCurrentAccountUserDataStore(@CurrentAccount currentAccount: UserId, userDataStoreProvider: UserDataStoreProvider) =
        userDataStoreProvider.getOrCreate(currentAccount)
}

@Module
@InstallIn(ServiceComponent::class)
class ServiceModule {
    @ServiceScoped
    @Provides
    @CurrentSessionFlowService
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSessionFlow
}

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions", "LargeClass")
class UseCaseModule {

    @ViewModelScoped
    @Provides
    fun provideObserveSyncStateUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @ViewModelScoped
    @Provides
    fun provideLogoutUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): LogoutUseCase =
        coreLogic.getSessionScope(currentAccount).logout

    @ViewModelScoped
    @Provides
    fun provideValidateEmailUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validateEmailUseCase

    @ViewModelScoped
    @Provides
    fun provideValidatePasswordUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validatePasswordUseCase

    @ViewModelScoped
    @Provides
    fun provideValidateUserHandleUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().validateUserHandleUseCase

    @ViewModelScoped
    @Provides
    fun provideGetServerConfigUserCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().fetchServerConfigFromDeepLink

    @ViewModelScoped
    @Provides
    fun provideUpdateApiVersionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().updateApiVersions

    @ViewModelScoped
    @Provides
    fun provideDisableEventProcessing(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).debug.disableEventProcessing

    @ViewModelScoped
    @Provides
    fun provideCurrentSessionFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSessionFlow

    @ViewModelScoped
    @Provides
    fun provideAddAuthenticatedUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic): AddAuthenticatedUserUseCase =
        coreLogic.getGlobalScope().addAuthenticatedAccount

    @ViewModelScoped
    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic
    ) = coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).persistPersistentWebSocketConnectionStatus

    @ViewModelScoped
    @Provides
    fun provideGetPersistentWebSocketStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).getPersistentWebSocketStatus

    @ViewModelScoped
    @Provides
    fun provideCheckCrlRevocationListUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).checkCrlRevocationList

    @ViewModelScoped
    @Provides
    fun provideIsMLSEnabledUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isMLSEnabled

    @ViewModelScoped
    @Provides
    fun provideGetDefaultProtocol(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).getDefaultProtocol

    @ViewModelScoped
    @Provides
    fun provideIsE2EIEnabledUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isE2EIEnabled

    @ViewModelScoped
    @Provides
    fun provideIsFileSharingEnabledUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).isFileSharingEnabled

    @ViewModelScoped
    @Provides
    fun provideFileSharingStatusFlowUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeFileSharingStatus

    @ViewModelScoped
    @Provides
    fun fileSystemProvider(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): KaliumFileSystem =
        coreLogic.getSessionScope(currentAccount).kaliumFileSystem

    @ViewModelScoped
    @Provides
    fun provideFederatedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FederatedIdMapper =
        coreLogic.getSessionScope(currentAccount).federatedIdMapper

    @ViewModelScoped
    @Provides
    fun provideQualifiedIdMapper(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): QualifiedIdMapper =
        coreLogic.getSessionScope(currentAccount).qualifiedIdMapper

    @ViewModelScoped
    @Provides
    fun provideBlockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): BlockUserUseCase = coreLogic.getSessionScope(currentAccount).connection.blockUser

    @ViewModelScoped
    @Provides
    fun provideUnblockUserUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UnblockUserUseCase = coreLogic.getSessionScope(currentAccount).connection.unblockUser

    @ViewModelScoped
    @Provides
    fun provideObserveValidAccountsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveValidAccountsUseCase =
        coreLogic.getGlobalScope().observeValidAccounts

    @ViewModelScoped
    @Provides
    fun provideDoesValidSessionExistsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): DoesValidSessionExistUseCase =
        coreLogic.getGlobalScope().doesValidSessionExist

    @ViewModelScoped
    @Provides
    fun observeSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).observeSecurityClassificationLabel

    @ViewModelScoped
    @Provides
    fun provideCreateBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).backup.create

    @ViewModelScoped
    @Provides
    fun provideVerifyBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).backup.verify

    @ViewModelScoped
    @Provides
    fun provideRestoreBackupUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).backup.restore

    @ViewModelScoped
    @Provides
    fun provideUpdateApiVersionsScheduler(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.updateApiVersionsScheduler

    @ViewModelScoped
    @Provides
    fun provideObserveIfAppFreshEnoughUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeIfAppUpdateRequired

    @ViewModelScoped
    @Provides
    fun provideMarkFileSharingStatusAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkFileSharingChangeAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markFileSharingStatusAsNotified

    @ViewModelScoped
    @Provides
    fun provideMarkSelfDeletingMessagesAsNotified(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): MarkSelfDeletionStatusAsNotifiedUseCase = coreLogic.getSessionScope(currentAccount).markSelfDeletingMessagesAsNotified

    @ViewModelScoped
    @Provides
    fun provideObserveTeamSettingsSelfDeletionStatusFlagUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveTeamSettingsSelfDeletingStatusUseCase = coreLogic.getSessionScope(currentAccount).observeTeamSettingsSelfDeletionStatus

    @ViewModelScoped
    @Provides
    fun provideObserveSelfDeletionTimerSettingsForConversationUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveSelfDeletionTimerSettingsForConversationUseCase = coreLogic.getSessionScope(currentAccount).observeSelfDeletingMessages

    @ViewModelScoped
    @Provides
    fun providePersistNewSelfDeletingMessagesUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistNewSelfDeletionTimerUseCase = coreLogic.getSessionScope(currentAccount).persistNewSelfDeletionStatus

    @ViewModelScoped
    @Provides
    fun provideImageUtil(): ImageUtil = ImageUtil

    @ViewModelScoped
    @Provides
    fun provideObserveGuestRoomLinkFeatureFlagUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeGuestRoomLinkFeatureFlag

    @ViewModelScoped
    @Provides
    fun provideMarkGuestLinkFeatureFlagAsNotChangedUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).markGuestLinkFeatureFlagAsNotChanged

    @ViewModelScoped
    @Provides
    fun provideMarkTeamAppLockStatusAsNotifiedUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).markTeamAppLockStatusAsNotified

    @ViewModelScoped
    @Provides
    fun provideGetOtherUserSecurityClassificationLabelUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveOtherUserSecurityClassificationLabelUseCase =
        coreLogic.getSessionScope(currentAccount).getOtherUserSecurityClassificationLabel

    @ViewModelScoped
    @Provides
    fun provideObserveNewClientsUseCaseUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().observeNewClientsUseCase

    @ViewModelScoped
    @Provides
    fun provideClearNewClientsForUser(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().clearNewClientsForUser

    @ViewModelScoped
    @Provides
    fun providePersistScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): PersistScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).persistScreenshotCensoringConfig

    @ViewModelScoped
    @Provides
    fun provideObserveScreenshotCensoringConfigUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): ObserveScreenshotCensoringConfigUseCase = coreLogic.getSessionScope(currentAccount).observeScreenshotCensoringConfig

    @ViewModelScoped
    @Provides
    fun provideObserveIsAppLockEditableUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic
    ): ObserveIsAppLockEditableUseCase = coreLogic.getGlobalScope().observeIsAppLockEditableUseCase

    @ViewModelScoped
    @Provides
    fun provideObserveLegalHoldRequestUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldRequest

    @ViewModelScoped
    @Provides
    fun provideObserveLegalHoldForSelfUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldForSelfUser

    @ViewModelScoped
    @Provides
    fun provideObserveLegalHoldForUserUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).observeLegalHoldStateForUser

    @ViewModelScoped
    @Provides
    fun provideMembersHavingLegalHoldClientUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).membersHavingLegalHoldClient

    @ViewModelScoped
    @Provides
    fun provideFetchConversationMLSVerificationStatusUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): FetchConversationMLSVerificationStatusUseCase = coreLogic.getSessionScope(currentAccount).fetchConversationMLSVerificationStatus

    @ViewModelScoped
    @Provides
    fun provideGetCurrentAnalyticsTrackingIdentifierUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): GetCurrentAnalyticsTrackingIdentifierUseCase = coreLogic.getSessionScope(currentAccount).getCurrentAnalyticsTrackingIdentifier

    @ViewModelScoped
    @Provides
    fun provideBreakSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): BreakSessionUseCase =
        coreLogic.getSessionScope(currentAccount).debug.breakSession

    @ViewModelScoped
    @Provides
    fun provideSendFCMTokenToAPIUseCase(@KaliumCoreLogic coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId) =
        coreLogic.getSessionScope(currentAccount).debug.sendFCMTokenToServer

    @ViewModelScoped
    @Provides
    fun provideMigrateFromPersonalToTeamUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).migrateFromPersonalToTeam

    @ViewModelScoped
    @Provides
    fun provideGetTeamUrlUseCase(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ) = coreLogic.getSessionScope(currentAccount).getTeamUrlUseCase
}
