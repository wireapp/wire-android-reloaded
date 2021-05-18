package com.wire.android.core.crypto

import androidx.test.filters.RequiresDevice
import com.wire.android.InstrumentationTest
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.ClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.exception.UnknownCryptoFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.framework.mock.InjectMockKsRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@RequiresDevice
class CryptoBoxClientTest : InstrumentationTest() {

    @get:Rule
    val injectMocksRule = InjectMockKsRule.create(this)

    @MockK
    private lateinit var propertyStorage: CryptoBoxClientPropertyStorage

    @MockK
    private lateinit var preKeyMapper: PreKeyMapper

    @MockK
    private lateinit var exceptionMapper: CryptoExceptionMapper

    lateinit var subject: CryptoBoxClient

    private val userId = UserId("abc")

    @Before
    fun setup() {
        subject = CryptoBoxClient(appContext, propertyStorage, userId, preKeyMapper, exceptionMapper)
    }

    @Test
    fun givenPreKeysAreNeeded_whenTheyAreCreated_thenTheStorageIsUpdated() {
        val preKey = PreKey(42, "data")
        every { preKeyMapper.fromCryptoBoxModel(any()) } returns preKey

        subject.createInitialPreKeys()
        verify(exactly = 1) {
            propertyStorage.updateLastPreKeyId(userId, any())
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenConverting_theMapperShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { preKeyMapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()
        generated.isRight shouldBe true

        generated.map {
            val allKeys = it.createdKeys + it.lastKey
            verify(exactly = allKeys.size) { preKeyMapper.fromCryptoBoxModel(any()) }
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenReturning_theMapperResultShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { preKeyMapper.fromCryptoBoxModel(any()) } returns preKey

        val generated = subject.createInitialPreKeys()
        generated.isRight shouldBe true

        generated.map {
            it.lastKey shouldBeEqualTo preKey
            it.createdKeys shouldContainSame generateSequence { preKey }
                .take(it.createdKeys.size)
                .toList()
        }
    }

    @Test
    fun givenPreKeysAreGenerated_whenStoring_theLastPreKeyIdShouldBeUsed() {
        val preKey = PreKey(42, "data")
        every { preKeyMapper.fromCryptoBoxModel(any()) } returns preKey

        val result = subject.createInitialPreKeys()
        result.isRight shouldBe true

        result.map {
            val lastKeyId = it.createdKeys.last().id
            verify {
                propertyStorage.updateLastPreKeyId(userId, lastKeyId)
            }
        }
    }

    @Test
    fun givenANonRecognizedSessionId_whenEncrypting_shouldFailReturningMappersResult() {
        val plainMessage = PlainMessage("Hello".toByteArray())

        val expectedFailure = UnknownCryptoFailure(Throwable())
        every { exceptionMapper.fromNativeException(any()) } returns expectedFailure

        val result = subject.encryptMessage(CryptoSessionId(UserId("Bob"), ClientId("A")), plainMessage) { Either.Right(Unit) }
        result.isLeft shouldBe true
        (result as Either.Left).a shouldBeEqualTo expectedFailure
    }
}
