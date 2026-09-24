package it.socialblock.data.settings

internal object OverlayMessagesCodec {
    fun encode(messages: List<String>): String = buildString {
        messages.forEach { message ->
            append(message.length)
            append(':')
            append(message)
        }
    }

    fun decode(value: String): List<String>? {
        val messages = mutableListOf<String>()
        var offset = 0
        var valid = true
        while (offset < value.length && valid) {
            val separator = value.indexOf(':', startIndex = offset)
            if (separator == -1) {
                valid = false
            } else {
                val length = value.substring(offset, separator).toIntOrNull()
                val messageStart = separator + 1
                if (length == null || length < 0 || length > value.length - messageStart) {
                    valid = false
                } else {
                    val messageEnd = messageStart + length
                    messages += value.substring(messageStart, messageEnd)
                    offset = messageEnd
                }
            }
        }
        return messages.takeIf { valid }
    }
}
