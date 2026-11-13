package ua.kpi.iot_lighting.prac4.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ua.kpi.iot_lighting.prac4.domain.model.Role

class MockTokenFactoryTest {
    @Test
    fun `created token parses and verifies within ttl`() {
        val token = MockTokenFactory.createToken(
            subject = "admin@kpi.ua",
            role = Role.ADMIN,
            nowEpochSec = 1_000_000L,
            ttlSec = 3600L,
        )
        val payload = MockTokenFactory.parseAndVerify(
            token = token,
            nowEpochSec = 1_001_000L,
            verifySignature = true,
        )
        assertNotNull(payload)
        assertEquals("admin@kpi.ua", payload?.subject)
        assertEquals(Role.ADMIN, payload?.role)
    }

    @Test
    fun `expired or tampered token returns null`() {
        val token = MockTokenFactory.createToken(
            subject = "operator@kpi.ua",
            role = Role.USER,
            nowEpochSec = 1_000L,
            ttlSec = 60L,
        )
        assertNull(MockTokenFactory.parseAndVerify(token, nowEpochSec = 2_000L))
        val tampered = token.dropLast(3) + "xyz"
        assertNull(MockTokenFactory.parseAndVerify(tampered, nowEpochSec = 1_010L, verifySignature = true))
        assertNull(MockTokenFactory.parseAndVerify("bad.token", nowEpochSec = 1_010L))
    }
}
