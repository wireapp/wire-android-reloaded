package com.wire.android.feature.auth.registration.personal.email

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.EmailTooShort
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var emailViewModel: CreatePersonalAccountEmailViewModel

    @Mock
    private lateinit var sendActivationCodeUseCase: SendEmailActivationCodeUseCase

    @Mock
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Before
    fun setUp() {
        emailViewModel = CreatePersonalAccountEmailViewModel(
            coroutinesTestRule.dispatcherProvider, validateEmailUseCase, sendActivationCodeUseCase
        )
    }

    @Test
    fun `given validateEmail is called, when the validation succeeds then isValidEmail should be true`() {
        coroutinesTestRule.runTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isTrue()
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailTooShort error then isValidEmail should be false`() {
        coroutinesTestRule.runTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailTooShort))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailInvalid error then isValidEmail should be false`() {
        coroutinesTestRule.runTest {
            `when`(validateEmailUseCase.run(any())).thenReturn(Either.Left(EmailInvalid))

            emailViewModel.validateEmail(TEST_EMAIL)

            assertThat(emailViewModel.isValidEmailLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given sendActivation is called, then calls SendEmailActivationCodeUseCase`() {
        coroutinesTestRule.runTest {
            val params = SendEmailActivationCodeParams(TEST_EMAIL)
            `when`(sendActivationCodeUseCase.run(params)).thenReturn(Either.Right(Unit))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue()
            verify(sendActivationCodeUseCase).run(params)
        }
    }

    @Test
    fun `given sendActivation is called, when use case is successful, then sets email to sendActivationCodeLiveData`() {
        coroutinesTestRule.runTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertRight {
                assertThat(it).isEqualTo(TEST_EMAIL)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns NetworkError, then sets NetworkErrorMsg to sendActivationCodeLiveData`() {
        coroutinesTestRule.runTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(NetworkConnection))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it).isEqualTo(NetworkErrorMessage)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailBlacklisted, then sets error message to sendActivationCodeLiveData`() {
        coroutinesTestRule.runTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailBlacklisted))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_blacklisted_error)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailInUse, then sets error message to sendActivationCodeLiveData`() {
        coroutinesTestRule.runTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(EmailInUse))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_with_email_email_in_use_error)
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns other error, then sets GeneralErrorMessage to sendActivationCodeLiveData`() {
        coroutinesTestRule.runTest {
            `when`(sendActivationCodeUseCase.run(any())).thenReturn(Either.Left(ServerError))

            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue().assertLeft {
                assertThat(it).isEqualTo(GeneralErrorMessage)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
