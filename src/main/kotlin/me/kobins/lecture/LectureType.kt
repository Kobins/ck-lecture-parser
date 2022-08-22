package me.kobins.lecture

enum class LectureType(val text: String) {
    NORMAL      ("일반"),
    BLOCK_10WEEK("블록식(10주)"),
    BLOCK_5WEEK ("블록식(5주)"),
    ;
    companion object {
        private val byName by lazy {
            values().associateBy { it.text }
        }
        fun fromText(string: String?): LectureType? {
            if(string == null) return null
            return byName[string]
        }
    }
}