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
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.client.RestClientException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserAccountServiceTest {
	private val localTransaction = mock(UserAccountLocalTransaction::class.java)
	private val travelWithdrawalClient = mock(TravelWithdrawalClient::class.java)
	private val service = UserAccountService(localTransaction, travelWithdrawalClient)

	@Test
	fun `active user is checked before Travel and local withdrawal transaction`() {
		service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)

		val order = inOrder(localTransaction, travelWithdrawalClient)
		order.verify(localTransaction).requireActiveUser(TestFixtures.USER_ID)
		order.verify(travelWithdrawalClient)
			.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		order.verify(localTransaction).completeWithdrawal(TestFixtures.USER_ID)
	}

	@Test
	fun `missing active user does not call Travel or complete withdrawal`() {
		doThrow(UserNotFoundException())
			.`when`(localTransaction)
			.requireActiveUser(TestFixtures.USER_ID)

		assertThrows<UserNotFoundException> {
			service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		}

		verifyNoInteractions(travelWithdrawalClient)
		verify(localTransaction, never()).completeWithdrawal(TestFixtures.USER_ID)
	}

	@Test
	fun `Travel failure does not start local withdrawal transaction`() {
		doThrow(TravelServiceUnavailableException(RestClientException("unavailable")))
			.`when`(travelWithdrawalClient)
			.prepareWithdrawal(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)

		assertThrows<TravelServiceUnavailableException> {
			service.deleteAccount(TestFixtures.USER_ID, AUTHORIZATION, REQUEST_ID)
		}

		verify(localTransaction, never()).completeWithdrawal(TestFixtures.USER_ID)
	}

	companion object {
		private const val AUTHORIZATION = "Bearer access-token"
		private const val REQUEST_ID = "018f1ed0-dead-beef-acde-0242ac120002"
	}
}

class UserAccountLocalTransactionTest {
	private val userRepository = mock(UserRepository::class.java)
	private val refreshTokenService = mock(RefreshTokenService::class.java)
	private val localTransaction = UserAccountLocalTransaction(
		userRepository,
		refreshTokenService,
		TestFixtures.FIXED_CLOCK,
	)

	@Test
	fun `active user check rejects missing user`() {
		`when`(userRepository.existsById(TestFixtures.USER_ID)).thenReturn(false)

		assertThrows<UserNotFoundException> {
			localTransaction.requireActiveUser(TestFixtures.USER_ID)
		}
	}

	@Test
	fun `local withdrawal soft deletes user then revokes every refresh token`() {
		val user = User(OAuthProvider.GOOGLE, "account-to-delete")
		`when`(userRepository.findById(TestFixtures.USER_ID)).thenReturn(Optional.of(user))
		`when`(userRepository.saveAndFlush(user)).thenReturn(user)

		localTransaction.completeWithdrawal(TestFixtures.USER_ID)

		assertTrue(user.isDeleted)
		assertEquals(TestFixtures.FIXED_INSTANT, user.deletedAt)
		val order = inOrder(userRepository, refreshTokenService)
		order.verify(userRepository).saveAndFlush(user)
		order.verify(refreshTokenService).revokeAll(TestFixtures.USER_ID)
	}
}
