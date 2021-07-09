package com.wire.android.feature.contact.datasources.local

import androidx.room.*

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contact WHERE id IN(:ids)")
    suspend fun contactsById(ids: Set<String>): List<ContactEntity>

    @Query("SELECT * FROM contact")
    suspend fun contacts(): List<ContactEntity>

    @Delete
    fun delete(contactEntity: ContactEntity)
}
