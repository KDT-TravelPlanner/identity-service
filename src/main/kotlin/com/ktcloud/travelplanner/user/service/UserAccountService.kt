package com.ktcloud.travelplanner.user.service

import com.ktcloud.travelplanner.auth.service.RefreshTokenService
import com.ktcloud.travelplanner.user.client.TravelWithdrawalClient
import com.ktcloud.travelplanner.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class UserAccountService(
	private val localTransaction: UserAccountLocalTransaction,
	private val travelWithdrawalClient: TravelWithdrawalClient,
) {
	fun deleteAccount(
		userId: UUID,
		authorization: String,
		requestId: String,
	) {
		localTransaction.requireActiveUser(userId)

		// Travel owns planner ownership and membership cleanup. Its operation must be idempotent,
		// because an Identity persistence failure can cause this request to be retried.
		travelWithdrawalClient.prepareWithdrawal(userId, authorization, requestId)
		localTransaction.completeWithdrawal(userId)
	}
}

@Service
class UserAccountLocalTransaction(
	private val userRepository: UserRepository,
	private val refreshTokenService: RefreshTokenService,
	@Qualifier("utcClock") private val clock: Clock,
) {
	@Transactional(readOnly = true)
	fun requireActiveUser(userId: UUID) {
		if (!userRepository.existsById(userId)) {
			throw UserNotFoundException()
		}
	}

	@Transactional
	fun completeWithdrawal(userId: UUID) {
		val user = userRepository.findById(userId)
			.orElseThrow(::UserNotFoundException)

		user.softDelete(Instant.now(clock))
		userRepository.saveAndFlush(user)
		refreshTokenService.revokeAll(userId)
	}
}
