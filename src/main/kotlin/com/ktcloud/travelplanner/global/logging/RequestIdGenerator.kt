package com.ktcloud.travelplanner.global.logging

import java.util.UUID

class RequestIdGenerator {
	fun generate(): String = UUID.randomUUID().toString()

	fun resolveOrGenerate(candidate: String?): String {
		if (candidate == null) {
			return generate()
		}
		val parsed = try {
			UUID.fromString(candidate)
		} catch (exception: IllegalArgumentException) {
			return generate()
		}
		return candidate.takeIf { parsed.toString() == it } ?: generate()
	}

	companion object {
		const val HEADER_NAME = "X-Request-Id"
		const val MDC_KEY = "requestId"
	}
}
