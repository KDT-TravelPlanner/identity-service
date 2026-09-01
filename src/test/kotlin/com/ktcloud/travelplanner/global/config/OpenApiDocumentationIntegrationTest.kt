package com.ktcloud.travelplanner.global.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ktcloud.travelplanner.IdentityServiceApplication
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest(
	classes = [IdentityServiceApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.MOCK,
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class OpenApiDocumentationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val objectMapper: ObjectMapper,
) {
	@Test
	fun `OpenAPI document exposes only versioned API contracts with JWT security`() {
		val response = mockMvc.get("/v3/api-docs")
			.andExpect {
				status { isOk() }
			}
			.andReturn()
			.response

		val document = objectMapper.readTree(response.contentAsString)
		assertEquals(OpenApiConfiguration.API_TITLE, document.path("info").path("title").asText())
		assertEquals(OpenApiConfiguration.API_VERSION, document.path("info").path("version").asText())
		assertBearerSecurity(document)
		assertDocumentedPaths(document)
		assertAuthenticationContracts(document)
		assertRequestParameters(document)
	}

	@Test
	fun `OpenAPI YAML and Swagger UI are available without authentication`() {
		mockMvc.get("/v3/api-docs.yaml")
			.andExpect {
				status { isOk() }
			}

		mockMvc.get("/swagger-ui.html")
			.andExpect {
				status { is3xxRedirection() }
				redirectedUrl("/swagger-ui/index.html")
			}

		mockMvc.get("/swagger-ui/index.html")
			.andExpect {
				status { isOk() }
				content { string(org.hamcrest.Matchers.containsString("Swagger UI")) }
			}
	}

	@Test
	fun `business API remains protected when documentation paths are public`() {
		mockMvc.get("/api/v1/users/00000000-0000-0000-0000-000000000001/summary")
			.andExpect {
				status { isUnauthorized() }
			}
	}

	private fun assertBearerSecurity(document: JsonNode) {
		val bearerScheme = document.path("components").path("securitySchemes").path("bearerAuth")
		assertEquals("http", bearerScheme.path("type").asText())
		assertEquals("bearer", bearerScheme.path("scheme").asText())
		assertEquals("JWT", bearerScheme.path("bearerFormat").asText())
		assertTrue(document.path("security").single().has("bearerAuth"))
	}

	private fun assertDocumentedPaths(document: JsonNode) {
		val paths = document.path("paths")
		val documentedPaths = paths.fieldNames().asSequence().toSet()
		assertTrue(documentedPaths.isNotEmpty())
		assertTrue(documentedPaths.all { it.startsWith("/api/v1/") })
		assertFalse("/api/ping" in documentedPaths)

		listOf(
			"/api/v1/auth/token/exchange",
			"/api/v1/users/me/profile",
			"/api/v1/users/me/profile-image/complete",
			"/api/v1/users/{userId}/summary",
			"/api/v1/users/lookup",
		).forEach { path -> assertTrue(path in documentedPaths, "Missing documented path: $path") }

		assertTrue(document.path("components").path("schemas").has("UserSummaryResponse"))
		assertTrue(document.path("components").path("schemas").has("ProfileImageUploadCompleteRequest"))
	}

	private fun assertAuthenticationContracts(document: JsonNode) {
		val exchangeSecurity = document.path("paths")
			.path("/api/v1/auth/token/exchange")
			.path("post")
			.path("security")
		assertTrue(exchangeSecurity.isArray)
		assertTrue(exchangeSecurity.isEmpty)

		val oauthSecurity = document.path("paths")
			.path("/api/v1/auth/oauth2/{provider}")
			.path("get")
			.path("security")
		assertTrue(oauthSecurity.isArray)
		assertTrue(oauthSecurity.isEmpty)

		val logout = document.path("paths").path("/api/v1/auth/logout").path("post")
		assertFalse(logout.has("security"))
	}

	private fun assertRequestParameters(document: JsonNode) {
		val lookupParameters = document.path("paths")
			.path("/api/v1/users/lookup")
			.path("get")
			.path("parameters")
			.mapNotNull { it.path("name").textValue() }
		assertFalse("principal" in lookupParameters)
		assertTrue("nickname" in lookupParameters)

		val refreshCookie = document.path("paths")
			.path("/api/v1/auth/token/refresh")
			.path("post")
			.path("parameters")
			.firstOrNull { it.path("name").asText() == "refresh_token" }
		assertNotNull(refreshCookie)
		assertEquals("cookie", refreshCookie.path("in").asText())
	}
}
