package com.ktcloud.travelplanner.user.client

import com.ktcloud.travelplanner.testsupport.TestFixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class TravelWithdrawalClientTest {
	@Test
	fun `forwards bearer authorization and request id to Travel`() {
		val builder = RestClient.builder().baseUrl(TRAVEL_BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		val client = HttpTravelWithdrawalClient(builder.build())
		server.expect(
			requestTo("$TRAVEL_BASE_URL/api/v1/internal/users/${TestFixtures.USER_ID}/withdrawal"),
		)
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, AUTHORIZATION))
			.andExpect(header(REQUEST_ID_HEADER, REQUEST_ID))
			.andRespond(withSuccess())

		client.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)

		server.verify()
	}

	@Test
	fun `maps non-successful Travel response to service unavailable`() {
		val builder = RestClient.builder().baseUrl(TRAVEL_BASE_URL)
		val server = MockRestServiceServer.bindTo(builder).build()
		val client = HttpTravelWithdrawalClient(builder.build())
		server.expect(requestTo("$TRAVEL_BASE_URL/api/v1/internal/users/${TestFixtures.USER_ID}/withdrawal"))
			.andRespond(withServerError())

		assertThrows<TravelServiceUnavailableException> {
			client.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		}
	}

	companion object {
		private const val TRAVEL_BASE_URL = "http://travel-service"
		private const val AUTHORIZATION = "Bearer access-token"
		private const val REQUEST_ID_HEADER = "X-Request-Id"
		private const val REQUEST_ID = "018f1ed0-dead-beef-acde-0242ac120002"
	}
}
