package io.jans.chip.ui.screens.unauthenticated.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginErrorState
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginState
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginUiEvent
import io.jans.chip.ui.screens.unauthenticated.login.state.emailOrMobileEmptyErrorState
import io.jans.chip.ui.screens.unauthenticated.login.state.passwordEmptyErrorState

/**
 * ViewModel for Login Screen
 */
class LoginViewModel : ViewModel() {

    var loginState = mutableStateOf(LoginState())
        private set

    /**
     * Function called on any login event [LoginUiEvent]
     */
    fun onUiEvent(loginUiEvent: LoginUiEvent) {
        when (loginUiEvent) {

            // Email/Mobile changed
            /*is LoginUiEvent.EmailOrMobileChanged -> {
                loginState.value = loginState.value.copy(
                    emailOrMobile = loginUiEvent.inputValue,
                    errorState = loginState.value.errorState.copy(
                        emailOrMobileErrorState = if (loginUiEvent.inputValue.trim().isNotEmpty())
                            ErrorState()
                        else
                            emailOrMobileEmptyErrorState
                    )
                )
            }

            // Password changed
            is LoginUiEvent.PasswordChanged -> {
                loginState.value = loginState.value.copy(
                    password = loginUiEvent.inputValue,
                    errorState = loginState.value.errorState.copy(
                        passwordErrorState = if (loginUiEvent.inputValue.trim().isNotEmpty())
                            ErrorState()
                        else
                            passwordEmptyErrorState
                    )
                )
            }*/

            // Submit Login
            is LoginUiEvent.Submit -> {
                //val inputsValidated = validateInputs()
                //if (inputsValidated) {
                // TODO Trigger login in authentication flow
                loginState.value = loginState.value.copy(isLoginSuccessful = true)
                //}
            }

            is LoginUiEvent.Logout -> {
                //val inputsValidated = validateInputs()
                //if (inputsValidated) {
                // TODO Trigger login in authentication flow
                loginState.value = loginState.value.copy(isLoginSuccessful = false)
                //}
            }

            else -> {}
        }
    }

    /**
     * Function to validate inputs
     * Ideally it should be on domain layer (usecase)
     * @return true -> inputs are valid
     * @return false -> inputs are invalid
     */
    private fun validateInputs(): Boolean {
        val emailOrMobileString = loginState.value.emailOrMobile.trim()
        val passwordString = loginState.value.password
        return when {

            // Email/Mobile empty
            emailOrMobileString.isEmpty() -> {
                loginState.value = loginState.value.copy(
                    errorState = LoginErrorState(
                        emailOrMobileErrorState = emailOrMobileEmptyErrorState
                    )
                )
                false
            }

            //Password Empty
            passwordString.isEmpty() -> {
                loginState.value = loginState.value.copy(
                    errorState = LoginErrorState(
                        passwordErrorState = passwordEmptyErrorState
                    )
                )
                false
            }

            // No errors
            else -> {
                // Set default error state
                loginState.value = loginState.value.copy(errorState = LoginErrorState())
                true
            }
        }
    }

}