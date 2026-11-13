package ua.kpi.iot_lighting.prac4.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.kpi.iot_lighting.prac4.data.error.ErrorHandler
import ua.kpi.iot_lighting.prac4.domain.model.UiError
import ua.kpi.iot_lighting.prac4.domain.repository.AuthRepository
import ua.kpi.iot_lighting.prac4.domain.validation.CredentialsValidator

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isSubmitting: Boolean = false,
    val error: UiError? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentSession.collect { session ->
                // Очищаємо поля форми при виході з акаунта, щоб наступний користувач бачив порожню форму (N3)
                if (session == null) {
                    _state.update { prev ->
                        if (prev.isSubmitting) prev else LoginUiState()
                    }
                }
            }
        }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, passwordError = null, error = null) }
    }

    fun fillDemoOperator() {
        _state.update {
            it.copy(
                email = "user@light.io",
                password = "user123",
                emailError = null,
                passwordError = null,
                error = null,
            )
        }
    }

    fun fillDemoAdmin() {
        _state.update {
            it.copy(
                email = "admin@light.io",
                password = "admin123",
                emailError = null,
                passwordError = null,
                error = null,
            )
        }
    }

    fun submit() {
        val current = _state.value
        val validation = CredentialsValidator.validate(current.email, current.password)
        if (!validation.isValid) {
            _state.update {
                it.copy(
                    emailError = validation.emailError,
                    passwordError = validation.passwordError,
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val result = authRepository.login(current.email.trim(), current.password)
            _state.update { prev ->
                if (result.isSuccess) {
                    LoginUiState()
                } else {
                    prev.copy(
                        isSubmitting = false,
                        error = result.exceptionOrNull()?.let { ErrorHandler.toUiError(it) },
                    )
                }
            }
        }
    }
}
