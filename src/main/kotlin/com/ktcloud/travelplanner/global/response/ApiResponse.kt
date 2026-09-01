package com.ktcloud.travelplanner.global.response

@Deprecated(
	message = "Use travel-common ApiResponse.",
	replaceWith = ReplaceWith("ApiResponse<T>", "com.ktcloud.travelplanner.common.response.ApiResponse"),
)
typealias ApiResponse<T> = com.ktcloud.travelplanner.common.response.ApiResponse<T>
