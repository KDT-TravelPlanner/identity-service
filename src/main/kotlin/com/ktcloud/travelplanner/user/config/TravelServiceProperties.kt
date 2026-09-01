package com.ktcloud.travelplanner.user.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("app.services.travel")
data class TravelServiceProperties(
	val baseUrl: URI,
	val connectTimeout: Duration = Duration.ofSeconds(2),
	val readTimeout: Duration = Duration.ofSeconds(5),
) {
	init {
		require(baseUrl.isAbsolute && baseUrl.host != null) {
			"Travel service base URL must be an absolute HTTP(S) URL."
		}
		require(baseUrl.scheme in HTTP_SCHEMES) {
			"Travel service base URL must use HTTP or HTTPS."
		}
		require(!connectTimeout.isZero && !connectTimeout.isNegative) {
			"Travel service connect timeout must be positive."
		}
		require(!readTimeout.isZero && !readTimeout.isNegative) {
			"Travel service read timeout must be positive."
		}
	}

	companion object {
		private val HTTP_SCHEMES = setOf("http", "https")
	}
}
