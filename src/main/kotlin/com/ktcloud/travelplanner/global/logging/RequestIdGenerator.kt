package com.ktcloud.travelplanner.global.logging

@Deprecated(
	message = "Use travel-common RequestIdGenerator.",
	replaceWith = ReplaceWith("RequestIdGenerator", "com.ktcloud.travelplanner.common.logging.RequestIdGenerator"),
)
typealias RequestIdGenerator = com.ktcloud.travelplanner.common.logging.RequestIdGenerator
