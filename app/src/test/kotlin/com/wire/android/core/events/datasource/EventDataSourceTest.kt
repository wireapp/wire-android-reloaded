package com.wire.android.core.events.datasource

import com.wire.android.UnitTest
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

class EventDataSourceTest : UnitTest() {

    @MockK
    private lateinit var notificationLocalDataSource: NotificationLocalDataSource

    @MockK
    private lateinit var notificationRemoteDataSource: NotificationRemoteDataSource

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var subject: EventDataSource

    @Before
    fun setUp() {
        subject = EventDataSource(TestCoroutineScope(), notificationLocalDataSource, notificationRemoteDataSource, sessionRepository)
    }

    @Test
    fun `given the current session id, when collecting events, then the current client ID should be used in remoteDataSource`() {
        val clientId = "CLIENT_ID"
        coEvery { sessionRepository.currentClientId() } returns Either.Right(clientId)
        coEvery { notificationRemoteDataSource.receiveEvents(clientId) } returns flowOf()
        coEvery { notificationRemoteDataSource.notificationsFlow(clientId, String.EMPTY) } returns flowOf()

        runBlockingTest { subject.events().collect() }

        coVerify(exactly = 1) { notificationRemoteDataSource.receiveEvents(clientId) }
        coVerify(exactly = 1) { notificationRemoteDataSource.notificationsFlow(clientId, String.EMPTY) }
    }
}