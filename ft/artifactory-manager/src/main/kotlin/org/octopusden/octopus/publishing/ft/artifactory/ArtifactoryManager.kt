package org.octopusden.octopus.publishing.ft.artifactory

import org.apache.http.HttpStatus
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.slf4j.LoggerFactory
import java.io.InputStream

private val logger = LoggerFactory.getLogger("org.octopusden.octopus.publishing.ft.ArtifactoryManager")

/**
 * Thin wrapper around the Artifactory REST client used by FTs to verify that
 * artifacts published by the plugin actually landed in Artifactory.
 *
 * Inspired by — but trimmed down from — the legacy
 * `octopus-rm-gradle-plugin/ft/artifactory-manager`.
 */
class ArtifactoryManager(
    baseUrl: String,
    username: String,
    password: String,
) : AutoCloseable {

    private val client: Artifactory = ArtifactoryClientBuilder.create()
        .setUrl("$baseUrl/artifactory")
        .setUsername(username)
        .setPassword(password)
        .build()

    /** GET an artifact by `repoKey` + `path`. Returns the response body bytes, or `null` if 404. */
    fun fetchArtifact(repoKey: String, path: String): ByteArray? {
        val req: ArtifactoryRequest = ArtifactoryRequestImpl()
            .apiUrl("$repoKey/$path")
            .method(ArtifactoryRequest.Method.GET)
            .responseType(ArtifactoryRequest.ContentType.ANY)
        val response = client.restCall(req)
        return when (response.statusLine.statusCode) {
            HttpStatus.SC_OK -> (response.rawBody as? InputStream)?.use { it.readBytes() }
                ?: response.rawBody.toString().toByteArray()
            HttpStatus.SC_NOT_FOUND -> null
            else -> {
                logger.error("Unexpected status {} fetching {}/{}", response.statusLine.statusCode, repoKey, path)
                throw RuntimeException("Unexpected response ${response.statusLine.statusCode} for $repoKey/$path")
            }
        }
    }

    /** Delete an artifact by `repoKey` + `path`. Best-effort; logs on non-2xx. */
    fun deleteArtifact(repoKey: String, path: String) {
        val req: ArtifactoryRequest = ArtifactoryRequestImpl()
            .apiUrl("$repoKey/$path")
            .method(ArtifactoryRequest.Method.DELETE)
        val response = client.restCall(req)
        val code = response.statusLine.statusCode
        if (code !in 200..299 && code != HttpStatus.SC_NOT_FOUND) {
            logger.warn("Delete returned status {} for {}/{}", code, repoKey, path)
        }
    }

    override fun close() {
        client.close()
    }
}
