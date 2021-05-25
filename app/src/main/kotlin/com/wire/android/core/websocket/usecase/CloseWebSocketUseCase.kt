package com.wire.android.core.websocket.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.core.websocket.data.WebSocketDataSource

class CloseWebSocketUseCase(private val repository: WebSocketDataSource) : UseCase<Unit, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Unit> = Either.Right(repository.closeSocket())
}
