package it.socialblock.domain.model

data class UsageSnapshot(val capturedAtMillis: Long, val usageByPackage: Map<String, Long>, val foregroundPackage: String?)
