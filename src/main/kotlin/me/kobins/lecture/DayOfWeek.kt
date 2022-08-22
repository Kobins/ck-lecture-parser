package me.kobins.lecture

enum class DayOfWeek(val text: String) {
    MONDAY      ("월"),
    TUESDAY     ("화"),
    WEDNESDAY   ("수"),
    THURSDAY    ("목"),
    FRIDAY      ("금"),
    ;

    companion object {
        private val byText by lazy {
            values().associateBy { it.text }
        }
        fun fromText(string: String?): DayOfWeek? {
            if(string == null) return null
            return byText[string]
        }
    }
}