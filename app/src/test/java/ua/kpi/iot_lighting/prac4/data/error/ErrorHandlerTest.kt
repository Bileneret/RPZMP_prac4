package ua.kpi.iot_lighting.prac4.data.error

import com.google.gson.JsonSyntaxException
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import ua.kpi.iot_lighting.prac4.domain.model.UiError

class ErrorHandlerTest {
    @Test
    fun `maps network exceptions and marks transient retryable`() {
        val timeout = SocketTimeoutException("timeout")
        assertTrue(ErrorHandler.toUiError(timeout) is UiError.Network)
        assertTrue(ErrorHandler.isTransient(timeout))
        assertTrue(ErrorHandler.isTransient(IOException("reset")))
    }

    @Test
    fun `maps http codes properly`() {
        val err401 = HttpException(Response.error<Any>(401, "".toResponseBody()))
        val err500 = HttpException(Response.error<Any>(500, "".toResponseBody()))
        val err422 = HttpException(Response.error<Any>(422, "".toResponseBody()))

        assertTrue(ErrorHandler.toUiError(err401) is UiError.Unauthorized)
        assertFalse(ErrorHandler.isTransient(err401))

        assertTrue(ErrorHandler.toUiError(err500) is UiError.Server)
        assertTrue(ErrorHandler.isTransient(err500))

        assertTrue(ErrorHandler.toUiError(err422) is UiError.Validation)
        assertFalse(ErrorHandler.isTransient(err422))
    }

    @Test
    fun `json syntax exception maps to non-retryable server error`() {
        val ui = ErrorHandler.toUiError(JsonSyntaxException("bad json"))
        assertTrue(ui is UiError.Server)
        assertFalse(ui.isRetryable)
    }
}
