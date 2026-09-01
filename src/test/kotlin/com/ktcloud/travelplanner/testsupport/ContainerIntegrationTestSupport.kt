package com.ktcloud.travelplanner.testsupport

import com.ktcloud.travelplanner.IdentityServiceApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(
	classes = [IdentityServiceApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@Import(TestcontainersConfiguration::class)
abstract class ContainerIntegrationTestSupport
