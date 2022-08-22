package me.kobins.lecture

enum class LectureNote(val text: String) {
    NEW_LECTURE("신규 교과목"),
    OFFLINE_POSSIBILITY_AFTER_6WEEKS("6주차부터 대면수업 전환 가능성 있음."),
    ;
    companion object {
        private val byText by lazy {
            values().associateBy { it.text }
        }
        fun fromText(string: String?): LectureNote? {
            if(string.isNullOrEmpty()) return null
            for((text, note) in byText) {
                if(string.contains(text)) return note
            }
            return null
        }
    }
}