package com.ktcloud.travelplanner.global.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RequestIdGeneratorTest {

	private val requestIdGenerator = RequestIdGenerator()

	@Test
	fun `each request receives a new canonical UUID v4`() {
		val firstRequestId = requestIdGenerator.generate()
		val secondRequestId = requestIdGenerator.generate()
		val firstUuid = UUID.fromString(firstRequestId)
		val secondUuid = UUID.fromString(secondRequestId)

		assertEquals(firstUuid.toString(), firstRequestId)
		assertEquals(secondUuid.toString(), secondRequestId)
		assertEquals(4, firstUuid.version())
		assertEquals(4, secondUuid.version())
		assertNotEquals(firstRequestId, secondRequestId)
	}

	@Test
	fun `canonical incoming UUID is preserved for service propagation`() {
		val incoming = "018f1ed0-dead-beef-acde-0242ac120002"

		assertEquals(incoming, requestIdGenerator.resolveOrGenerate(incoming))
	}

	@Test
	fun `invalid incoming request id is replaced`() {
		val resolved = requestIdGenerator.resolveOrGenerate("untrusted\r\nrequest-id")

		UUID.fromString(resolved)
		assertNotEquals("untrusted\r\nrequest-id", resolved)
	}
}
