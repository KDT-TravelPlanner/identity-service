package com.ktcloud.travelplanner.user.client

import com.ktcloud.travelplanner.global.exception.ErrorCode
import com.ktcloud.travelplanner.global.exception.ExternalServiceException
import com.ktcloud.travelplanner.global.logging.RequestIdGenerator
import com.ktcloud.travelplanner.user.config.TravelHttpClientConfiguration
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

interface TravelWithdrawalClient {
	fun prepareWithdrawal(
		userId: UUID,
		authorization: String,
		requestId: String,
	)
}

@Component
class HttpTravelWithdrawalClient(
	@Qualifier(TravelHttpClientConfiguration.TRAVEL_REST_CLIENT)
	private val restClient: RestClient,
) : TravelWithdrawalClient {
	override fun prepareWithdrawal(
		userId: UUID,
		authorization: String,
		requestId: String,
	) {
		try {
			restClient.post()
				.uri(WITHDRAWAL_PATH, userId)
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.header(RequestIdGenerator.HEADER_NAME, requestId)
				.retrieve()
				.toBodilessEntity()
		} catch (exception: RestClientException) {
			throw TravelServiceUnavailableException(exception)
		}
	}

	companion object {
		const val WITHDRAWAL_PATH = "/api/v1/internal/users/{userId}/withdrawal"
	}
}

class TravelServiceUnavailableException(
	cause: Throwable,
) : ExternalServiceException(ErrorCode.TRAVEL_SERVICE_UNAVAILABLE, cause = cause)
