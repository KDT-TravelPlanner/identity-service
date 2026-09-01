package com.ktcloud.travelplanner.user.service

import com.ktcloud.travelplanner.user.dto.UserSummaryResponse
import com.ktcloud.travelplanner.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserSummaryService(
	private val userRepository: UserRepository,
) {
	@Transactional(readOnly = true)
	fun getById(userId: UUID): UserSummaryResponse =
		userRepository.findById(userId)
			.map(UserSummaryResponse::from)
			.orElseThrow(::UserNotFoundException)

	@Transactional(readOnly = true)
	fun getByNickname(nickname: String): UserSummaryResponse =
		userRepository.findByNickname(nickname)
			?.let(UserSummaryResponse::from)
			?: throw UserNotFoundException()
}
