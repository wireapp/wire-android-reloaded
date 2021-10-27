package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NoEntityFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.content.EncryptedMessageEnvelope
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.CombinedMessageContactEntity
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.user.QualifiedId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageDataSourceTest : UnitTest() {

    private lateinit var messageDataSource: MessageDataSource

    @MockK
    private lateinit var messageLocalDataSource: MessageLocalDataSource

    @MockK
    private lateinit var messageMapper: MessageMapper

    @MockK
    private lateinit var contactMapper: ContactMapper

    @MockK
    private lateinit var cryptoBoxClient: CryptoBoxClient

    @MockK
    private lateinit var cryptoSession: CryptoSessionId

    @MockK
    private lateinit var encryptedMessage: EncryptedMessage

    @Before
    fun setUp() {
        messageDataSource = MessageDataSource(messageLocalDataSource, messageMapper, contactMapper, cryptoBoxClient)
    }

    @Test
    fun `given decryptMessage is called, when clientId is null, then do not decrypt message`() {
        val message = mockk<EncryptedMessageEnvelope>().also {
            every { it.clientId } returns null
        }

        runBlocking { messageDataSource.receiveEncryptedMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage) { _ -> Either.Right(Unit) } }
    }

    @Test
    fun `given decryptMessage is called, when decoded content is null, then do not decrypt message`() {
        mockkStatic(Base64::class)
        every { Base64.decode(TEST_CONTENT, Base64.DEFAULT) } returns null
        val message = mockk<EncryptedMessageEnvelope>().also {
            every { it.clientId } returns TEST_CLIENT_ID
            every { it.content } returns TEST_CONTENT
        }

        runBlocking { messageDataSource.receiveEncryptedMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage) { _ -> Either.Right(Unit) } }
    }

    @Test
    fun `given decryptMessage is called, when message is valid, then it should be decrypted`() {
        val plainMessage = PlainMessage(byteArrayOf())
        mockkStatic(Base64::class)
        every { Base64.decode(TEST_CONTENT, Base64.DEFAULT) } returns byteArrayOf()
        val message = mockk<EncryptedMessageEnvelope>().also {
            every { it.clientId } returns TEST_CLIENT_ID
            every { it.senderUserId } returns TEST_USER_ID
            every { it.content } returns TEST_CONTENT
        }

        every { messageMapper.cryptoSessionFromEncryptedEnvelope(message) } returns cryptoSession
        every { messageMapper.encryptedMessageFromDecodedContent(byteArrayOf()) } returns encryptedMessage
        val lambdaSlot = slot<(suspend (PlainMessage) -> Either<Failure, Unit>)>()

        runBlocking { messageDataSource.receiveEncryptedMessage(message) }

        coVerify { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage, capture(lambdaSlot)) }
        runBlocking { lambdaSlot.captured.invoke(plainMessage) }
        verify(exactly = 1) { messageMapper.cryptoSessionFromEncryptedEnvelope(message) }
        verify(exactly = 1) { messageMapper.encryptedMessageFromDecodedContent(byteArrayOf()) }
        verify(exactly = 1) { messageMapper.toDecryptedMessage(message, plainMessage) }
        coVerify(exactly = 1) { messageLocalDataSource.save(any()) }
    }

    @Test
    fun `given conversationMessages is called, when messageLocalDataSource emits messages, then propagates mapped items`() {
        val conversationId = "conversation-id"
        val contactEntity = mockk<ContactEntity>()
        val messageEntity = mockk<MessageEntity>()
        val combinedMessageContactEntity = mockk<CombinedMessageContactEntity>().also {
            every { it.messageEntity } returns messageEntity
            every { it.contactEntity } returns contactEntity
        }
        val message = mockk<Message>()
        val contact = mockk<Contact>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        every { contactMapper.fromContactEntity(contactEntity) } returns contact
        coEvery { messageLocalDataSource.messagesByConversationId(conversationId) } returns flowOf(listOf(combinedMessageContactEntity))

        runBlocking {
            val result = messageDataSource.conversationMessages(conversationId)

            with(result.first()) {
                size shouldBeEqualTo 1
                get(0).message shouldBeEqualTo message
                get(0).contact shouldBeEqualTo contact
            }
        }
    }

    @Test
    fun `given localdatasource returns failure, when getting latest unread messages, then returns the failure`() {
        coEvery { messageLocalDataSource.latestUnreadMessagesByConversationId(any(), any()) } returns Either.Left(NoEntityFound)

        val result = runBlocking { messageDataSource.latestUnreadMessages(any()) }

        result shouldFail { }
        coVerify(exactly = 1) { messageLocalDataSource.latestUnreadMessagesByConversationId(any(), any()) }
    }

    @Test
    fun `given localdatasource returns entities, when getting latest unread messages, then maps the return and return the result`() {
        val contactEntity = mockk<ContactEntity>()
        val messageEntity = mockk<MessageEntity>()
        val combinedMessageContactEntity = mockk<CombinedMessageContactEntity>().also {
            every { it.messageEntity } returns messageEntity
            every { it.contactEntity } returns contactEntity
        }
        val message = mockk<Message>()
        val contact = mockk<Contact>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        every { contactMapper.fromContactEntity(contactEntity) } returns contact
        coEvery {
            messageLocalDataSource.latestUnreadMessagesByConversationId(any(), any())
        } returns Either.Right(listOf(combinedMessageContactEntity))

        val result = runBlocking { messageDataSource.latestUnreadMessages(any()) }

        result shouldSucceed {
            it[0].message shouldBeEqualTo message
            it[0].contact shouldBeEqualTo contact
        }
    }

    @Test
    fun `given localDataSource returns an entity, when getting message by id, then return the mapped result`() {
        val messageId = "someID2"
        val messageEntity = mockk<MessageEntity>()
        val message = mockk<Message>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        coEvery { messageLocalDataSource.messageById(messageId) } returns Either.Right(messageEntity)

        val result = runBlocking { messageDataSource.messageById(messageId) }

        result shouldSucceed {
            it shouldBeEqualTo message
        }
    }

    @Test
    fun `given localDataSource returns an entity, when getting message by id, then the local data should be mapped`() {
        val messageId = "someID2"
        val messageEntity = mockk<MessageEntity>()
        val message = mockk<Message>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        coEvery { messageLocalDataSource.messageById(messageId) } returns Either.Right(messageEntity)

        runBlocking { messageDataSource.messageById(messageId) }

        verify(exactly = 1) { messageMapper.fromEntityToMessage(messageEntity) }
    }

    @Test
    fun `given localDataSource returns a failure, when getting message by id, then the failure should be forwarded`() {
        val messageId = "someID2"
        val messageEntity = mockk<MessageEntity>()
        val message = mockk<Message>()
        val failure: Failure = mockk()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        coEvery { messageLocalDataSource.messageById(messageId) } returns Either.Left(failure)

        val result = runBlocking { messageDataSource.messageById(messageId) }

        result.shouldFail {
            it shouldBeEqualTo failure
        }
    }

    @Test
    fun `given a crypto session, when checking if session exists, then pass session to CryptoBoxClient`() {
        every { cryptoBoxClient.doesSessionExists(any()) } returns Either.Right(false)

        runBlocking {
            messageDataSource.doesCryptoSessionExists(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value
            )
        }

        coVerify(exactly = 1) { cryptoBoxClient.doesSessionExists(TEST_CRYPTO_SESSION) }
    }

    @Test
    fun `given cryptoBoxClient fails, when checking if session exists, then return failure`() {
        val failure = mockk<Failure>()
        every { cryptoBoxClient.doesSessionExists(any()) } returns Either.Left(failure)

        runBlocking {
            messageDataSource.doesCryptoSessionExists(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value
            )
        } shouldFail {
            it shouldBeEqualTo failure
        }
    }

    @Test
    fun `given session does not exist, when checking if session exists, then return false`() {
        every { cryptoBoxClient.doesSessionExists(any()) } returns Either.Right(false)

        runBlocking {
            messageDataSource.doesCryptoSessionExists(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value
            )
        } shouldSucceed {
            it shouldBeEqualTo false
        }
    }

    @Test
    fun `given session exist, when checking if session exists, then return true`() {
        every { cryptoBoxClient.doesSessionExists(any()) } returns Either.Right(true)

        runBlocking {
            messageDataSource.doesCryptoSessionExists(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value
            )
        } shouldSucceed {
            it shouldBeEqualTo true
        }
    }

    @Test
    fun `given a cryptoSession and a preKey, when establishing a session, then pass them to cryptoBoxClient`() {
        every { cryptoBoxClient.createSessionIfNeeded(any(), any()) } returns Either.Right(Unit)

        runBlocking {
            messageDataSource.establishCryptoSession(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value, TEST_PRE_KEY
            )
        }

        coVerify(exactly = 1) { cryptoBoxClient.createSessionIfNeeded(TEST_CRYPTO_SESSION, TEST_PRE_KEY) }
    }

    @Test
    fun `given cryptoBox succeeds, when establishing a session, then return right`() {
        every { cryptoBoxClient.createSessionIfNeeded(any(), any()) } returns Either.Right(Unit)

        runBlocking {
            messageDataSource.establishCryptoSession(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value, TEST_PRE_KEY
            )
        } shouldSucceed {}
    }

    @Test
    fun `given cryptoBox fails, when establishing a session, then forward failure`() {
        val failure = mockk<Failure>()
        every { cryptoBoxClient.createSessionIfNeeded(any(), any()) } returns Either.Left(failure)

        runBlocking {
            messageDataSource.establishCryptoSession(
                "self-user-id", TEST_CRYPTO_SESSION.userId.id, TEST_CRYPTO_SESSION.cryptoClientId.value, TEST_PRE_KEY
            )
        } shouldFail {
            it shouldBeEqualTo failure
        }
    }


    companion object {
        private const val TEST_CLIENT_ID = "client-id"
        private const val TEST_USER_ID = "user-id"
        private const val TEST_CONTENT = "This-is-a-content"
        private const val TEST_DOMAIN = "domain"
        private val TEST_CRYPTO_SESSION = CryptoSessionId(QualifiedId(TEST_DOMAIN, TEST_USER_ID), CryptoClientId(TEST_CLIENT_ID))
        private val TEST_PRE_KEY = PreKey(42, "key-data")
    }
}
