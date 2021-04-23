package com.wire.android.crypto

import com.wire.android.base.InstrumentationTest
import com.wire.android.crypto.mapper.PreKeyMapper
import com.wire.android.crypto.model.PreKey
import com.wire.android.crypto.storage.PreKeyRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CryptoBoxClientTest : InstrumentationTest() {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @MockK
    private lateinit var repository: PreKeyRepository

    @MockK
    private lateinit var mapper: PreKeyMapper

    private val rootFolder: File
        get() = temporaryFolder.root

    lateinit var subject: CryptoBoxClient

    @Before
    fun setup() {
        subject = CryptoBoxClient(rootFolder, repository, mapper)
    }

    @Test
    fun givenPreKeysAreNeeded_whenTheyAreCreated_thenTheRepositoryIsUpdated() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        subject.createInitialPreKeys()
        verify(exactly = 1) {
            repository.updateLastPreKeyID(any())
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenConverting_theMapperShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()

        val allKeys = generated.createdKeys + generated.lastKey
        verify(exactly = allKeys.size) { mapper.fromCryptoBoxModel(any()) }
    }

    @Test
    fun givenPreKeysAreGenerated_whenReturning_theMapperResultShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()

        generated.lastKey shouldBeEqualTo preKey
        generated.createdKeys shouldContainSame generateSequence { preKey }
            .take(generated.createdKeys.size)
            .toList()
    }

    @Test
    fun givenPreKeysAreGenerated_whenStoring_theLastPreKeyIdShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { mapper.fromCryptoBoxModel(any()) } returns preKey

        val result = subject.createInitialPreKeys()
        val lastKeyId = result.createdKeys.last().id
        verify {
            repository.updateLastPreKeyID(lastKeyId)
        }
    }

    @Test
    fun givenDeleteIsNeeded_whenItIsCalled_ThenTheFolderShouldBeCleared() {
        val subFile = File(rootFolder, "anotherFile")
        subFile.mkdirs()

        rootFolder.listFiles()!!.size shouldBeEqualTo 1

        subject.delete()

        rootFolder.exists() shouldBeEqualTo false
    }
}
