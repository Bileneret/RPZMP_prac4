package ua.kpi.iot_lighting.prac4.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsValidatorTest {
    @Test
    fun `valid email and password pass validation`() {
        val result = CredentialsValidator.validate("operator@kpi.ua", "1234")
        assertTrue(result.isValid)
        assertEquals(null, result.emailError)
        assertEquals(null, result.passwordError)
    }

    @Test
    fun `invalid email and short password fail validation`() {
        val result = CredentialsValidator.validate("not-an-email", "12")
        assertFalse(result.isValid)
        assertNotNull(result.emailError)
        assertNotNull(result.passwordError)
    }

    @Test
    fun `blank inputs return explicit messages`() {
        val result = CredentialsValidator.validate("   ", "")
        assertFalse(result.isValid)
        assertNotNull(result.emailError)
        assertNotNull(result.passwordError)
    }
}
