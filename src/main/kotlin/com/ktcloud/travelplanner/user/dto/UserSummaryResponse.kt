package com.ktcloud.travelplanner.user.dto

import com.ktcloud.travelplanner.user.model.User
import java.util.UUID

data class UserSummaryResponse(
	val userId: UUID,
	val nickname: String?,
	val profileImageUrl: String?,
) {
	companion object {
		fun from(user: User): UserSummaryResponse = UserSummaryResponse(
			userId = requireNotNull(user.id),
			nickname = user.nickname,
			profileImageUrl = user.profileImageUrl,
		)
	}
}
