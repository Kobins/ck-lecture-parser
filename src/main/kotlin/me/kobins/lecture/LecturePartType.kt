package me.kobins.lecture

enum class LecturePartType {
    O,
    P,
    Q,
    R,
    S,
    T,
    U,
    V,
    W,
    X,
    Y,
    Z,
    ;
    companion object {
        private val byName by lazy {
            values().associateBy { it.name }
        }
        fun fromName(string: String?): LecturePartType? {
            if(string == null) return null
            return byName[string]
        }
    }
}