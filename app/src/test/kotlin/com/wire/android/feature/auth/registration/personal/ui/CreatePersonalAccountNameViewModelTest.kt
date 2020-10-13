package com.wire.android.feature.auth.registration.personal.ui

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.name.NameTooShort
import com.wire.android.shared.user.name.ValidateNameParams
import com.wire.android.shared.user.name.ValidateNameUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class CreatePersonalAccountNameViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var validateNameUseCase: ValidateNameUseCase

    private lateinit var nameViewModel: CreatePersonalAccountNameViewModel

    @Before
    fun setUp() {
        nameViewModel = CreatePersonalAccountNameViewModel(coroutinesTestRule.dispatcherProvider, validateNameUseCase)
    }

    @Test
    fun `given validateName() is called with a name, when use case returns success, then sets continueEnabled to true`() {
        coroutinesTestRule.runTest {
            `when`(validateNameUseCase.run(ValidateNameParams(TEST_NAME))).thenReturn(Either.Right(Unit))

            nameViewModel.validateName(TEST_NAME)

            assertThat(nameViewModel.continueEnabled.awaitValue()).isTrue()
            verify(validateNameUseCase).run(ValidateNameParams(TEST_NAME))
        }
    }

    @Test
    fun `given validateName() is called with a name, when use case fails, then sets continueEnabled to false`() {
        coroutinesTestRule.runTest {
            `when`(validateNameUseCase.run(ValidateNameParams(TEST_NAME))).thenReturn(Either.Left(NameTooShort))

            nameViewModel.validateName(TEST_NAME)

            assertThat(nameViewModel.continueEnabled.awaitValue()).isFalse()
            verify(validateNameUseCase).run(ValidateNameParams(TEST_NAME))
        }
    }

    companion object {
        private const val TEST_NAME = "Name Surname"
    }
}
