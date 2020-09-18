package com.wire.android.feature.auth.login.email

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.user.UserSession

interface LoginRepository {
    suspend fun loginWithEmail(email: String, password: String) : Either<Failure, UserSession>
}
