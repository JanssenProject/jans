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
        val username = loginState.value.username.trim()
        val passwordString = loginState.value.password
        return when {

            // Email/Mobile empty
            username.isEmpty() -> {
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