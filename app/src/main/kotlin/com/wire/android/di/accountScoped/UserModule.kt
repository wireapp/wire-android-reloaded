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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.DeleteAssetUseCase
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetAvatarAssetUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollment
import com.wire.kalium.logic.feature.conversation.GetAllContactsNotInConversationUseCase
import com.wire.kalium.logic.feature.e2ei.SyncCertificateRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMembersE2EICertificateStatusesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificatesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.ObserveCertificateRevocationForSelfClientUseCase
import com.wire.kalium.logic.feature.featureConfig.FeatureFlagsSyncWorker
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.GetKnownUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class UserModule {

    @Provides
    @ViewModelScoped
    fun provideUserScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): UserScope = coreLogic.getSessionScope(currentAccount).users

    @ViewModelScoped
    @Provides
    fun provideRefreshUsersWithoutMetadataUseCase(
        userScope: UserScope
    ): RefreshUsersWithoutMetadataUseCase = userScope.refreshUsersWithoutMetadata

    @ViewModelScoped
    @Provides
    fun provideDeleteAccountUseCase(
        userScope: UserScope
    ): DeleteAccountUseCase =
        userScope.deleteAccount

    @ViewModelScoped
    @Provides
    fun provideUpdateEmailUseCase(
        userScope: UserScope
    ): UpdateEmailUseCase =
        userScope.updateEmail

    @ViewModelScoped
    @Provides
    fun provideUpdateDisplayNameUseCase(
        userScope: UserScope
    ): UpdateDisplayNameUseCase =
        userScope.updateDisplayName

    @ViewModelScoped
    @Provides
    fun provideGetAssetSizeLimitUseCase(
        userScope: UserScope
    ): GetAssetSizeLimitUseCase =
        userScope.getAssetSizeLimit

    @ViewModelScoped
    @Provides
    fun provideObserveReadReceiptsEnabled(userScope: UserScope): ObserveReadReceiptsEnabledUseCase =
        userScope.observeReadReceiptsEnabled

    @ViewModelScoped
    @Provides
    fun providePersistReadReceiptsStatusConfig(userScope: UserScope): PersistReadReceiptsStatusConfigUseCase =
        userScope.persistReadReceiptsStatusConfig

    @ViewModelScoped
    @Provides
    fun provideFinalizeMLSClientAfterE2EIEnrollmentUseCase(userScope: UserScope): FinalizeMLSClientAfterE2EIEnrollment =
        userScope.finalizeMLSClientAfterE2EIEnrollment

    @ViewModelScoped
    @Provides
    fun provideObserveTypingIndicatorEnabled(userScope: UserScope): ObserveTypingIndicatorEnabledUseCase =
        userScope.observeTypingIndicatorEnabled

    @ViewModelScoped
    @Provides
    fun providePersistTypingIndicatorStatusConfig(userScope: UserScope): PersistTypingIndicatorStatusConfigUseCase =
        userScope.persistTypingIndicatorStatusConfig

    @ViewModelScoped
    @Provides
    fun provideSelfServerConfig(
        userScope: UserScope
    ): SelfServerConfigUseCase = userScope.serverLinks

    @ViewModelScoped
    @Provides
    fun provideObserveUserInfoUseCase(
        userScope: UserScope
    ): ObserveUserInfoUseCase = userScope.observeUserInfo

    @ViewModelScoped
    @Provides
    fun provideIsPasswordRequiredUseCase(
        userScope: UserScope
    ): IsPasswordRequiredUseCase = userScope.isPasswordRequired

    @ViewModelScoped
    @Provides
    fun provideIsReadOnlyAccountUseCase(
        userScope: UserScope
    ): IsReadOnlyAccountUseCase = userScope.isReadOnlyAccount

    @ViewModelScoped
    @Provides
    fun provideGetAllContactsNotInTheConversationUseCase(
        userScope: UserScope
    ): GetAllContactsNotInConversationUseCase =
        userScope.getAllContactsNotInConversation

    @ViewModelScoped
    @Provides
    fun provideGetUserInfoUseCase(userScope: UserScope): GetUserInfoUseCase =
        userScope.getUserInfo

    @ViewModelScoped
    @Provides
    fun provideUpdateSelfAvailabilityStatusUseCase(userScope: UserScope): UpdateSelfAvailabilityStatusUseCase =
        userScope.updateSelfAvailabilityStatus

    @ViewModelScoped
    @Provides
    fun provideGetAllContactsUseCase(
        userScope: UserScope
    ): GetAllContactsUseCase =
        userScope.getAllKnownUsers

    @ViewModelScoped
    @Provides
    fun provideGetKnownUserUseCase(
        userScope: UserScope
    ): GetKnownUserUseCase =
        userScope.getKnownUser

    @ViewModelScoped
    @Provides
    fun provideGetSelfUseCase(userScope: UserScope): GetSelfUserUseCase =
        userScope.getSelfUser

    @ViewModelScoped
    @Provides
    fun provideGetAvatarAssetUseCase(userScope: UserScope): GetAvatarAssetUseCase =
        userScope.getPublicAsset

    @ViewModelScoped
    @Provides
    fun provideDeleteAssetUseCase(userScope: UserScope): DeleteAssetUseCase =
        userScope.deleteAsset

    @ViewModelScoped
    @Provides
    fun provideUploadUserAvatarUseCase(userScope: UserScope): UploadUserAvatarUseCase =
        userScope.uploadUserAvatar

    @ViewModelScoped
    @Provides
    fun provideSetUserHandleUseCase(userScope: UserScope): SetUserHandleUseCase =
        userScope.setUserHandle

    @ViewModelScoped
    @Provides
    fun provideGetE2EICertificateUseCase(userScope: UserScope): GetMLSClientIdentityUseCase =
        userScope.getE2EICertificate

    @ViewModelScoped
    @Provides
    fun provideGetUserE2eiCertificateStatusUseCase(userScope: UserScope): IsOtherUserE2EIVerifiedUseCase =
        userScope.getUserE2eiCertificateStatus

    @ViewModelScoped
    @Provides
    fun provideGetMembersE2EICertificateStatusesUseCase(userScope: UserScope): GetMembersE2EICertificateStatusesUseCase =
        userScope.getMembersE2EICertificateStatuses

    @ViewModelScoped
    @Provides
    fun provideGetUserE2eiCertificates(userScope: UserScope): GetUserE2eiCertificatesUseCase =
        userScope.getUserE2eiCertificates

    @ViewModelScoped
    @Provides
    fun provideCertificateRevocationListCheckWorker(userScope: UserScope): SyncCertificateRevocationListUseCase =
        userScope.syncCertificateRevocationListUseCase

    @ViewModelScoped
    @Provides
    fun provideFeatureFlagsSyncWorker(userScope: UserScope): FeatureFlagsSyncWorker =
        userScope.featureFlagsSyncWorker

    @ViewModelScoped
    @Provides
    fun provideIsPersonalToTeamAccountSupportedByBackendUseCase(userScope: UserScope): CanMigrateFromPersonalToTeamUseCase =
        userScope.isPersonalToTeamAccountSupportedByBackend

    @ViewModelScoped
    @Provides
    fun provideObserveCertificateRevocationForSelfClientUseCase(userScope: UserScope): ObserveCertificateRevocationForSelfClientUseCase =
        userScope.observeCertificateRevocationForSelfClient
}
