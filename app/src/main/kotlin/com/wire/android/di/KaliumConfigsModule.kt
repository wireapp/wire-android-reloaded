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
import android.os.Build
import com.wire.android.BuildConfig
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.kalium.logic.featureFlags.BuildFileRestrictionState
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class KaliumConfigsModule {

    @Provides
    fun provideKaliumConfigs(context: Context): KaliumConfigs {
        val fileRestriction: BuildFileRestrictionState = if (BuildConfig.FILE_RESTRICTION_ENABLED) {
            BuildConfig.FILE_RESTRICTION_LIST.split(",").map { it.trim() }.let {
                BuildFileRestrictionState.AllowSome(it)
            }
        } else {
            BuildFileRestrictionState.NoRestriction
        }

        return KaliumConfigs(
            fileRestrictionState = fileRestriction,
            forceConstantBitrateCalls = BuildConfig.FORCE_CONSTANT_BITRATE_CALLS,
            // we use upsert, available from SQL3.24, which is supported from Android API30, so for older APIs we have to use SQLCipher
            shouldEncryptData = !BuildConfig.DEBUG || Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
            lowerKeyPackageLimits = BuildConfig.LOWER_KEYPACKAGE_LIMIT,
            lowerKeyingMaterialsUpdateThreshold = BuildConfig.PRIVATE_BUILD,
            developmentApiEnabled = BuildConfig.DEVELOPMENT_API_ENABLED,
            ignoreSSLCertificatesForUnboundCalls = BuildConfig.IGNORE_SSL_CERTIFICATES,
            encryptProteusStorage = true,
            guestRoomLink = BuildConfig.ENABLE_GUEST_ROOM_LINK,
            selfDeletingMessages = BuildConfig.SELF_DELETING_MESSAGES,
            wipeOnCookieInvalid = BuildConfig.WIPE_ON_COOKIE_INVALID,
            wipeOnDeviceRemoval = BuildConfig.WIPE_ON_DEVICE_REMOVAL,
            wipeOnRootedDevice = BuildConfig.WIPE_ON_ROOTED_DEVICE,
            isWebSocketEnabledByDefault = isWebsocketEnabledByDefault(context),
            certPinningConfig = BuildConfig.CERTIFICATE_PINNING_CONFIG,
            maxRemoteSearchResultCount = BuildConfig.MAX_REMOTE_SEARCH_RESULT_COUNT,
            limitTeamMembersFetchDuringSlowSync = BuildConfig.LIMIT_TEAM_MEMBERS_FETCH_DURING_SLOW_SYNC
        )
    }
}
