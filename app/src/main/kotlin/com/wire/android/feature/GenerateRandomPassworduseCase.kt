/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.feature

import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.internal.addHeaderLenient
import javax.inject.Inject
import kotlin.random.Random

@ViewModelScoped
class GenerateRandomPasswordUseCase @Inject constructor() {

    operator fun invoke(): String {

        val passwordLength = Random.nextInt(MIN_LENGTH, MAX_LENGTH + 1)

        return buildList<Char> {
            add(lowercase[Random.nextInt(lowercase.length)])
            add(uppercase[Random.nextInt(uppercase.length)])
            add(digits[Random.nextInt(digits.length)])
            add(specialChars[Random.nextInt(specialChars.length)])

            repeat(passwordLength - 4) {
                add(allCharacters[Random.nextInt(allCharacters.length)])
            }
        }.shuffled().joinToString("")
    }

    private companion object {
        const val lowercase: String = "abcdefghijklmnopqrstuvwxyz"
        const val uppercase: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val digits: String = "0123456789"
        const val specialChars: String = "!@#$%^&*()_+[]{}|;:,.<>?-"
        const val allCharacters: String = lowercase + uppercase + digits + specialChars

        const val MIN_LENGTH = 15
        const val MAX_LENGTH = 20
    }
}
