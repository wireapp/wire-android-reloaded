package com.wire.android.feature.conversation.data.remote

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.list.usecase.Conversation
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class ConversationDataSourceTest : UnitTest() {

    private lateinit var conversationsDataSource: ConversationsRepository

    @MockK
    private lateinit var conversationRemoteDataSource: ConversationRemoteDataSource

    @MockK
    private lateinit var conversationMapper: ConversationMapper

    @Before
    fun setup() {
        conversationsDataSource = ConversationDataSource(conversationMapper, conversationRemoteDataSource)
    }

    @Test
    fun `given conversationsByBatch is requested, when remoteDataSource returns success then returns list of conversations`() {
        val conversationsResponse: ConversationsResponse = mockk(relaxed = true)
        val conversation = mockk<Conversation>(relaxed = true)
        val conversationList = listOf(conversation, conversation)

        coEvery {
            conversationRemoteDataSource.conversationsByBatch(
                start = TEST_START,
                size = TEST_SIZE,
                ids = TEST_IDS
            )
        } returns Either.Right(conversationsResponse)
        every { conversationMapper.fromConversationResponse(conversationsResponse) } returns conversationList

        val result = runBlocking {
            conversationsDataSource.conversationsByBatch(
                TEST_START,
                TEST_SIZE,
                TEST_IDS
            )
        }

        result shouldSucceed { it shouldBe conversationList }
        verify(exactly = 1) { conversationMapper.fromConversationResponse(conversationsResponse) }
    }

    @Test
    fun `given conversationsByBatch is requested, when remoteDataSource returns a failed response, then propagates error upwards`() {
        coEvery {
            conversationRemoteDataSource.conversationsByBatch(
                start = TEST_START,
                size = TEST_SIZE,
                ids = TEST_IDS
            )
        } returns Either.Left(ServerError)

        val result = runBlocking {
            conversationsDataSource.conversationsByBatch(
                TEST_START,
                TEST_SIZE,
                TEST_IDS
            )
        }

        result shouldFail { it shouldBe ServerError }
    }

    companion object {
        private const val TEST_START = "87dehhe883=jdgegge7730"
        private const val TEST_SIZE = 10
        private val TEST_IDS = emptyList<String>()
    }
}
