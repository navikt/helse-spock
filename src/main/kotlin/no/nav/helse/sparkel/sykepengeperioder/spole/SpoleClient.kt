package no.nav.helse.sparkel.sykepengeperioder.spole

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import kotlinx.io.core.Input
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

class SpoleClient(private val baseUrl: String, private val accesstokenScope: String, private val azureClient: AzureClient) {

    companion object {
        private val objectMapper = ObjectMapper()
        private val tjenestekallLog = LoggerFactory.getLogger("tjenestekall")
    }

    fun hentSykepengeperioder(aktørId: String, periodeTom: LocalDate): Sykepengeperioder {
        val url = "$baseUrl/sykepengeperioder/${aktørId}?periodeTom=$periodeTom"
        val (responseCode, responseBody) = with(URL(url).openConnection() as HttpURLConnection) {
            requestMethod = "GET"

            setRequestProperty("Authorization", "Bearer ${azureClient.getToken(accesstokenScope).accessToken}")
            setRequestProperty("Accept", "application/json")

            val stream: InputStream? = if (responseCode < 300) this.inputStream else this.errorStream
            responseCode to stream?.bufferedReader()?.readText()
        }

        tjenestekallLog.info("svar fra spole: url=$url responseCode=$responseCode responseBody=$responseBody")

        if (responseCode >= 300 || responseBody == null) {
            throw RuntimeException("unknown error (responseCode=$responseCode) from spole")
        }

        val jsonNode = objectMapper.readTree(responseBody)

        return Sykepengeperioder(
                aktørId = jsonNode["aktør_id"].textValue(),
                perioder = (jsonNode["perioder"] as ArrayNode).map { periodeJson ->
                    Periode(
                            fom = LocalDate.parse(periodeJson["fom"].textValue()),
                            tom = LocalDate.parse(periodeJson["tom"].textValue()),
                            grad = periodeJson["sykemeldingsgrad"].textValue()
                    )
                }
        )
    }
}

data class Sykepengeperioder(val aktørId: String, val perioder: List<Periode>)
data class Periode(val fom: LocalDate, val tom: LocalDate, val grad: String)
