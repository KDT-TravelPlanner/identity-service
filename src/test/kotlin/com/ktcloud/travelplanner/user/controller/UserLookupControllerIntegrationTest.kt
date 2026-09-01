package com.ktcloud.travelplanner.user.controller

import com.ktcloud.travelplanner.global.security.JwtTokenService
import com.ktcloud.travelplanner.testsupport.TestFixtures
import com.ktcloud.travelplanner.testsupport.TestcontainersConfiguration
import com.ktcloud.travelplanner.user.model.OAuthProvider
import com.ktcloud.travelplanner.user.model.User
import com.ktcloud.travelplanner.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertNotNull

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
@Transactional
class UserLookupControllerIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val userRepository: UserRepository,
	@Autowired private val jwtTokenService: JwtTokenService,
	@Autowired private val entityManager: EntityManager,
) {
	@Test
	fun `summary and exact nickname lookup expose only community-safe fields`() {
		val user = saveUser("lookup-user")
		val authorization = authorizationFor(user)

		mockMvc.get("/api/v1/users/${user.id}/summary") {
			header(HttpHeaders.AUTHORIZATION, authorization)
		}
			.andExpect {
				status { isOk() }
				jsonPath("$.data.userId", equalTo(user.id.toString()))
				jsonPath("$.data.nickname", equalTo("lookup-user"))
				jsonPath("$.data.profileImageUrl", equalTo(PROFILE_IMAGE_URL))
				jsonPath("$.data.email") { doesNotExist() }
				jsonPath("$.data.name") { doesNotExist() }
			}

		mockMvc.get("/api/v1/users/lookup") {
			header(HttpHeaders.AUTHORIZATION, authorization)
			param("nickname", "lookup-user")
		}
			.andExpect {
				status { isOk() }
				jsonPath("$.data.userId", equalTo(user.id.toString()))
				jsonPath("$.data.nickname", equalTo("lookup-user"))
			}
	}

	@Test
	fun `soft deleted users are not returned by either lookup`() {
		val requester = saveUser("requester")
		val deleted = saveUser("deleted-user").also {
			it.softDelete(TestFixtures.FIXED_INSTANT)
			userRepository.saveAndFlush(it)
		}
		entityManager.clear()
		val authorization = authorizationFor(requester)

		mockMvc.get("/api/v1/users/${deleted.id}/summary") {
			header(HttpHeaders.AUTHORIZATION, authorization)
		}
			.andExpect {
				status { isNotFound() }
				jsonPath("$.code", equalTo("RESOURCE_NOT_FOUND"))
			}

		mockMvc.get("/api/v1/users/lookup") {
			header(HttpHeaders.AUTHORIZATION, authorization)
			param("nickname", "deleted-user")
		}
			.andExpect {
				status { isNotFound() }
				jsonPath("$.code", equalTo("RESOURCE_NOT_FOUND"))
			}
	}

	@Test
	fun `user lookup requires authentication`() {
		mockMvc.get("/api/v1/users/${UUID.randomUUID()}/summary")
			.andExpect { status { isUnauthorized() } }
	}

	private fun saveUser(nickname: String): User = userRepository.saveAndFlush(
		User(
			provider = OAuthProvider.GOOGLE,
			providerUserId = "lookup-${UUID.randomUUID()}",
			email = "$nickname@example.com",
			name = nickname,
		).also {
			it.completeProfile(nickname, null, null)
			it.updateOAuthProfile(it.email, it.name, PROFILE_IMAGE_URL)
		},
	)

	private fun authorizationFor(user: User): String =
		"Bearer ${jwtTokenService.issueAccessToken(assertNotNull(user.id)).value}"

	companion object {
		private const val PROFILE_IMAGE_URL = "https://images.example/profile.png"
	}
}
