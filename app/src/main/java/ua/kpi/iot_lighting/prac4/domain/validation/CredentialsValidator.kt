package ua.kpi.iot_lighting.prac4.domain.validation

data class ValidationOutcome(
    val emailError: String? = null,
    val passwordError: String? = null,
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null
}

object CredentialsValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validate(email: String, password: String): ValidationOutcome {
        val trimmedEmail = email.trim()
        val emailError = when {
            trimmedEmail.isEmpty() -> "Введіть електронну пошту"
            !EMAIL_REGEX.matches(trimmedEmail) -> "Некоректний формат email"
            else -> null
        }

        val passwordError = when {
            password.isBlank() -> "Введіть пароль"
            password.length < 4 -> "Пароль має містити щонайменше 4 символи"
            else -> null
        }

        return ValidationOutcome(
            emailError = emailError,
            passwordError = passwordError,
        )
    }
}
