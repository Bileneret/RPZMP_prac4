package ua.kpi.iot_lighting.prac4.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenStoreTest {
    private lateinit var store: TokenStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = TokenStore(context)
        store.clear()
    }

    @Test
    fun `write read and clear persist token across instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNull(store.read())

        store.write("header.payload.sig")
        assertEquals("header.payload.sig", TokenStore(context).read())

        store.clear()
        assertNull(TokenStore(context).read())
    }
}
