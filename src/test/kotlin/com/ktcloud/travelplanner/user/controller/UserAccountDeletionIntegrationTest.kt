package com.ktcloud.travelplanner.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ktcloud.travelplanner.auth.controller.RefreshTokenCookieFactory
import com.ktcloud.travelplanner.auth.repository.RedisRefreshTokenStore
import com.ktcloud.travelplanner.auth.repository.RefreshTokenHasher
import com.ktcloud.travelplanner.auth.service.OAuthExchangeCodeService
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import com.ktcloud.travelplanner.user.client.TravelServiceUnavailableException
import com.ktcloud.travelplanner.user.client.TravelWithdrawalClient
import com.ktcloud.travelplanner.user.model.OAuthProvider
import com.ktcloud.travelplanner.user.model.User
import com.ktcloud.travelplanner.user.repository.UserRepository
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockCookie
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class UserAccountDeletionIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val userRepository: UserRepository,
	@Autowired private val exchangeCodeService: OAuthExchangeCodeService,
	@Autowired private val objectMapper: ObjectMapper,
	@Autowired private val redisTemplate: StringRedisTemplate,
	@Autowired private val refreshTokenHasher: RefreshTokenHasher,
	@Autowired private val jdbcTemplate: JdbcTemplate,
) {
	@MockitoBean
	private lateinit var travelWithdrawalClient: TravelWithdrawalClient

	@Test
	fun `account deletion forwards auth and request id then revokes every device`() {
		val user = saveUser()
		val firstDevice = login(user)
		val secondDevice = login(user)
		val firstTokenKey = tokenKey(firstDevice.refreshCookie.value)
		val secondTokenKey = tokenKey(secondDevice.refreshCookie.value)
		assertTrue(redisTemplate.hasKey(firstTokenKey))
		assertTrue(redisTemplate.hasKey(secondTokenKey))

		val authorization = "Bearer ${firstDevice.accessToken}"
		val deleteResponse = mockMvc.delete("/api/v1/users/me") {
			header(HttpHeaders.AUTHORIZATION, authorization)
			header(REQUEST_ID_HEADER, REQUEST_ID)
			cookie(firstDevice.refreshCookie)
		}
			.andExpect {
				status { isOk() }
				jsonPath("$.data") { exists() }
				header { string(REQUEST_ID_HEADER, REQUEST_ID) }
				header { exists(HttpHeaders.SET_COOKIE) }
			}
			.andReturn()
			.response

		verify(travelWithdrawalClient).prepareWithdrawal(
			requireNotNull(user.id),
			authorization,
			REQUEST_ID,
		)
		val expiredCookie = MockCookie.parse(assertNotNull(deleteResponse.getHeader(HttpHeaders.SET_COOKIE)))
		assertEquals("", expiredCookie.value)
		assertEquals(Duration.ZERO.seconds.toInt(), expiredCookie.maxAge)
		assertEquals(RefreshTokenCookieFactory.COOKIE_PATH, expiredCookie.path)
		assertFalse(redisTemplate.hasKey(firstTokenKey))
		assertFalse(redisTemplate.hasKey(secondTokenKey))
		assertTrue(
			jdbcTemplate.queryForObject(
				"SELECT deleted_at IS NOT NULL FROM user_table WHERE id = ?",
				Boolean::class.java,
				user.id,
			) == true,
		)

		mockMvc.get("/api/v1/users/me/profile") {
			header(HttpHeaders.AUTHORIZATION, authorization)
		}
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.code", equalTo("UNAUTHORIZED"))
			}

		assertRefreshRejected(firstDevice.refreshCookie)
		assertRefreshRejected(secondDevice.refreshCookie)
	}

	@Test
	fun `Travel failure returns 503 without deleting user or revoking tokens`() {
		val user = saveUser()
		val authentication = login(user)
		val authorization = "Bearer ${authentication.accessToken}"
		doThrow(TravelServiceUnavailableException(RestClientException("down")))
			.`when`(travelWithdrawalClient)
			.prepareWithdrawal(requireNotNull(user.id), authorization, REQUEST_ID)

		mockMvc.delete("/api/v1/users/me") {
			header(HttpHeaders.AUTHORIZATION, authorization)
			header(REQUEST_ID_HEADER, REQUEST_ID)
			cookie(authentication.refreshCookie)
		}
			.andExpect {
				status { isServiceUnavailable() }
				jsonPath("$.code", equalTo("TRAVEL_SERVICE_UNAVAILABLE"))
				header { doesNotExist(HttpHeaders.SET_COOKIE) }
			}

		assertTrue(userRepository.existsById(requireNotNull(user.id)))
		assertTrue(redisTemplate.hasKey(tokenKey(authentication.refreshCookie.value)))
	}

	@Test
	fun `account deletion requires authentication`() {
		mockMvc.delete("/api/v1/users/me")
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.code", equalTo("UNAUTHORIZED"))
			}
	}

	private fun saveUser(): User = userRepository.saveAndFlush(
		User(
			provider = OAuthProvider.GOOGLE,
			providerUserId = "delete-account-${UUID.randomUUID()}",
		),
	)

	private fun login(user: User): AuthenticationFixture {
		val code = exchangeCodeService.issue(requireNotNull(user.id))
		val response = mockMvc.post("/api/v1/auth/token/exchange") {
			contentType = MediaType.APPLICATION_JSON
			content = objectMapper.writeValueAsString(mapOf("code" to code))
		}
			.andExpect { status { isOk() } }
			.andReturn()
			.response

		return AuthenticationFixture(
			accessToken = objectMapper.readTree(response.contentAsByteArray).at("/data/accessToken").asText(),
			refreshCookie = MockCookie.parse(assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE))),
		)
	}

	private fun assertRefreshRejected(cookie: MockCookie) {
		mockMvc.post("/api/v1/auth/token/refresh") { cookie(cookie) }
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.code", equalTo("INVALID_REFRESH_TOKEN"))
			}
	}

	private fun tokenKey(token: String): String =
		"${RedisRefreshTokenStore.TOKEN_KEY_PREFIX}:${refreshTokenHasher.hash(token)}"

	private data class AuthenticationFixture(
		val accessToken: String,
		val refreshCookie: MockCookie,
	)

	companion object {
		private const val REQUEST_ID_HEADER = "X-Request-Id"
		private const val REQUEST_ID = "018f1ed0-dead-beef-acde-0242ac120002"
	}
}
