package com.ktcloud.travelplanner.user.controller

import com.ktcloud.travelplanner.global.response.ApiResponse
import com.ktcloud.travelplanner.user.dto.UserSummaryResponse
import com.ktcloud.travelplanner.user.service.UserSummaryService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1/users")
class UserLookupController(
	private val userSummaryService: UserSummaryService,
) {
	@GetMapping("/{userId}/summary")
	fun getSummary(
		@PathVariable userId: UUID,
	): ApiResponse<UserSummaryResponse> =
		ApiResponse.success(userSummaryService.getById(userId))

	@GetMapping("/lookup")
	fun findByNickname(
		@RequestParam
		@NotBlank(message = "비어 있을 수 없습니다.")
		@Size(max = 30, message = "30자 이하여야 합니다.")
		nickname: String,
	): ApiResponse<UserSummaryResponse> =
		ApiResponse.success(userSummaryService.getByNickname(nickname))
}
