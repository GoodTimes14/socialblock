package it.socialblock.usage

data class UsageEventRecord(val packageName: String, val timestampMillis: Long, val type: UsageEventType)

enum class UsageEventType {
    FOREGROUND,
    BACKGROUND,
}
