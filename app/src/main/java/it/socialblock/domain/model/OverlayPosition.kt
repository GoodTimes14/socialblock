package it.socialblock.domain.model

enum class OverlayPosition {
    TOP,
    CENTER,
    BOTTOM,
    ;

    companion object {
        fun fromStoredValue(value: String?): OverlayPosition = entries.firstOrNull { it.name == value } ?: CENTER
    }
}
