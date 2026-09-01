package com.ktcloud.travelplanner

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdentityPingResponseFactoryTest {

	@Test
	fun `configured application name and first profile are mapped`() {
		val response = PingResponseFactory.create(
			applicationName = "identity-service",
			activeProfiles = arrayOf("dev", "local"),
		)

		assertEquals("ok", response.status)
		assertEquals("identity-service", response.application)
		assertEquals("dev", response.profile)
	}

	@Test
	fun `missing values use Identity defaults`() {
		val response = PingResponseFactory.create(
			applicationName = null,
			activeProfiles = emptyArray(),
		)

		assertEquals("identity-service", response.application)
		assertEquals("default", response.profile)
	}
}
