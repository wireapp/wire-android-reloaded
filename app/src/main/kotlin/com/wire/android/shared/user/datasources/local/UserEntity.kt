package com.wire.android.shared.user.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
        @PrimaryKey val id: String,
        val name: String,
        val email: String? = null,
        val username: String? = null,
        @ColumnInfo(name="asset_key")
        val assetKey: String? = null
)
