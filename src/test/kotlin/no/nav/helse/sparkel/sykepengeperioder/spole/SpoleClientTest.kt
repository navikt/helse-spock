package no.nav.helse.sparkel.sykepengeperioder.spole

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SpoleClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    private val spoleClient: SpoleClient

    init {
        val azureClientMock = mockk<AzureClient>()
        every {
            azureClientMock.getToken("scope")
        } returns AzureClient.Token(
                tokenType = "Bearer",
                expiresIn = 3600,
                accessToken = "foobar"
        )

        spoleClient = SpoleClient(baseUrl = server.baseUrl(), accesstokenScope = "scope", azureClient = azureClientMock)
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `henter sykepengeperioder fra spole`() {
        stubFor(spoleRequestMapping
                .willReturn(WireMock.ok(ok_sykepengeperioder_response)))

        val sykepengeperioder = spoleClient.hentSykepengeperioder(aktørId = aktørId, periodeTom = periodeTom)

        assertEquals(1, sykepengeperioder.perioder.size)
        assertEquals(LocalDate.parse("2019-01-01"), sykepengeperioder.perioder[0].fom)
        assertEquals(LocalDate.parse("2019-02-01"), sykepengeperioder.perioder[0].tom)
        assertEquals("100", sykepengeperioder.perioder[0].grad)
    }
}

private val aktørId = "123456789123"
private val periodeTom = LocalDate.of(2019, 1, 1)

private val spoleRequestMapping = get(urlPathEqualTo("/sykepengeperioder/$aktørId"))
        .withQueryParam("periodeTom", equalTo(periodeTom.toString()))
        .withHeader("Authorization", equalTo("Bearer foobar"))
        .withHeader("Accept", equalTo("application/json"))

private val ok_sykepengeperioder_response = """
{
    "aktør_id": "$aktørId",
    "perioder": [
        {
            "fom": "2019-01-01",
            "tom": "2019-02-01",
            "sykemeldingsgrad": "100"
        }   
    ]
}
}""".trimIndent()

