package com.ktcloud.travelplanner.user.controller

import com.ktcloud.travelplanner.auth.controller.RefreshTokenCookieFactory
import com.ktcloud.travelplanner.global.logging.RequestLoggingContext
import com.ktcloud.travelplanner.global.response.ApiResponse
import com.ktcloud.travelplanner.global.security.AuthenticatedUserPrincipal
import com.ktcloud.travelplanner.user.dto.ProfileImageUploadCompleteRequest
import com.ktcloud.travelplanner.user.dto.ProfileImageUploadUrlRequest
import com.ktcloud.travelplanner.user.dto.ProfileImageUploadUrlResponse
import com.ktcloud.travelplanner.user.dto.UserProfileResponse
import com.ktcloud.travelplanner.user.dto.UserProfileUpdateRequest
import com.ktcloud.travelplanner.user.service.ProfileImageUploadService
import com.ktcloud.travelplanner.user.service.UserAccountService
import com.ktcloud.travelplanner.user.service.UserProfileService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
class UserController(
	private val profileImageUploadService: ProfileImageUploadService,
	private val refreshTokenCookieFactory: RefreshTokenCookieFactory,
	private val userAccountService: UserAccountService,
	private val userProfileService: UserProfileService,
) {
	@GetMapping("/profile")
	fun getProfile(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
	): ApiResponse<UserProfileResponse> =
		ApiResponse.success(userProfileService.getProfile(principal.userId))

	@PatchMapping("/profile")
	fun updateProfile(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
		@Valid @RequestBody request: UserProfileUpdateRequest,
	): ApiResponse<UserProfileResponse> =
		ApiResponse.success(userProfileService.updateProfile(principal.userId, request))

	@PostMapping("/profile-image/upload-url")
	fun createProfileImageUploadUrl(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
		@Valid @RequestBody request: ProfileImageUploadUrlRequest,
	): ApiResponse<ProfileImageUploadUrlResponse> =
		ApiResponse.success(profileImageUploadService.createUploadUrl(principal.userId, request))

	@PostMapping("/profile-image/complete")
	fun completeProfileImageUpload(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
		@Valid @RequestBody request: ProfileImageUploadCompleteRequest,
	): ApiResponse<UserProfileResponse> =
		ApiResponse.success(profileImageUploadService.completeUpload(principal.userId, request))

	@DeleteMapping
	fun deleteAccount(
		@AuthenticationPrincipal principal: AuthenticatedUserPrincipal,
		@RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
		request: HttpServletRequest,
		response: HttpServletResponse,
	): ApiResponse<Unit> {
		userAccountService.deleteAccount(
			userId = principal.userId,
			authorization = authorization,
			requestId = requireNotNull(RequestLoggingContext.getRequestId(request)),
		)
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expire().toString())
		return ApiResponse.success(Unit)
	}
}
