package com.ktcloud.travelplanner.user.config

import com.ktcloud.travelplanner.global.external.externalHttpRequestFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
class TravelHttpClientConfiguration {
	@Bean
	@Qualifier(TRAVEL_REST_CLIENT)
	fun travelRestClient(
		restClientBuilder: RestClient.Builder,
		properties: TravelServiceProperties,
	): RestClient = restClientBuilder.clone()
		.baseUrl(properties.baseUrl.toASCIIString().trimEnd('/'))
		.requestFactory(
			externalHttpRequestFactory(
				connectTimeout = properties.connectTimeout,
				readTimeout = properties.readTimeout,
			),
		)
		.build()

	companion object {
		const val TRAVEL_REST_CLIENT = "travelRestClient"
	}
}
