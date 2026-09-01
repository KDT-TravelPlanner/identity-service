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
	private val userRepository: UserRepository,
	private val refreshTokenService: RefreshTokenService,
	private val travelWithdrawalClient: TravelWithdrawalClient,
	@Qualifier("utcClock") private val clock: Clock,
) {
	@Transactional
	fun deleteAccount(
		userId: UUID,
		authorization: String,
		requestId: String,
	) {
		val user = userRepository.findById(userId)
			.orElseThrow(::UserNotFoundException)

		// Travel owns planner ownership and membership cleanup. Its operation must be idempotent,
		// because an Identity persistence failure can cause this request to be retried.
		travelWithdrawalClient.prepareWithdrawal(userId, authorization, requestId)

		user.softDelete(Instant.now(clock))
		userRepository.saveAndFlush(user)
		refreshTokenService.revokeAll(userId)
	}
}
