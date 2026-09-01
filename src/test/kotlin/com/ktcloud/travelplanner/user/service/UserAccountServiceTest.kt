package com.ktcloud.travelplanner.user.service

import com.ktcloud.travelplanner.auth.service.RefreshTokenService
import com.ktcloud.travelplanner.testsupport.TestFixtures
import com.ktcloud.travelplanner.user.client.TravelServiceUnavailableException
import com.ktcloud.travelplanner.user.client.TravelWithdrawalClient
import com.ktcloud.travelplanner.user.model.OAuthProvider
import com.ktcloud.travelplanner.user.model.User
import com.ktcloud.travelplanner.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.client.RestClientException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserAccountServiceTest {
	private val userRepository = mock(UserRepository::class.java)
	private val refreshTokenService = mock(RefreshTokenService::class.java)
	private val travelWithdrawalClient = mock(TravelWithdrawalClient::class.java)
	private val service = UserAccountService(
		userRepository,
		refreshTokenService,
		travelWithdrawalClient,
		TestFixtures.FIXED_CLOCK,
	)

	@Test
	fun `Travel success is followed by soft delete and all refresh token revocation`() {
		val user = User(OAuthProvider.GOOGLE, "account-to-delete")
		`when`(userRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.of(user))
		`when`(userRepository.saveAndFlush(user)).thenReturn(user)

		service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)

		assertTrue(user.isDeleted)
		assertEquals(TestFixtures.FIXED_INSTANT, user.deletedAt)
		val order = inOrder(travelWithdrawalClient, userRepository, refreshTokenService)
		order.verify(travelWithdrawalClient)
			.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		order.verify(userRepository).saveAndFlush(user)
		order.verify(refreshTokenService).revokeAll(TestFixtures.USER_ID)
	}

	@Test
	fun `missing active user does not call Travel or revoke tokens`() {
		`when`(userRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.empty())

		assertThrows<UserNotFoundException> {
			service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		}

		verifyNoInteractions(travelWithdrawalClient, refreshTokenService)
	}

	@Test
	fun `Travel failure leaves Identity user and tokens unchanged`() {
		val user = User(OAuthProvider.GOOGLE, "account-to-keep")
		`when`(userRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.of(user))
		doThrow(TravelServiceUnavailableException(RestClientException("unavailable")))
			.`when`(travelWithdrawalClient)
			.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)

		assertThrows<TravelServiceUnavailableException> {
			service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		}

		assertFalse(user.isDeleted)
		verifyNoInteractions(refreshTokenService)
	}

	companion object {
		private const val AUTHORIZATION = "Bearer access-token"
		private const val REQUEST_ID = "018f1ed0-dead-beef-acde-0242ac120002"
	}
}
