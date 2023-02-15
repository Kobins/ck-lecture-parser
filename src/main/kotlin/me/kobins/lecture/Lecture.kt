package me.kobins.lecture

class Lecture(
    /** 요일 */
    val dayOfWeek: DayOfWeek,
    /** 교시 */
    val duration: List<IntRange>,
    /** 강의 이름 */
    val name: String,
    /** 분반 */
    val partType: LecturePartType,
    /** 교수명 */
    val professorName: String,
    /** 필수여부 */
    val isEssential: Boolean,
    /** 학점 */
    val grades: Int,
    /** 시수 */
    val times: Int,
    /** 패스제 여부 */
    val isPass: Boolean,
    /** 수업구분(일반, 블록식) */
    val type: LectureType,
    /** 수업주차 */
    val weekDuration: IntRange,
    /** 비대면 여부 */
    val isOnline: Boolean,
    /** 인원수 */
    val numberOfPeople: Int,
    /** 강의실 */
    val place: String,
    /** 비고 */
    val note: LectureNote?,
) {
    companion object {
        fun parse(raw: String?): Lecture? {
            if(raw == null) return null
            fun error(cause: String) {
                printlnWarn("변환 실패(${cause}): $raw")
            }
            val data = raw.split(' ')
            if(data.size < 15){
                error("길이 부족(${data.size})")
                return null
            }
            val index = data[0].toIntOrNull()
            if(index == null){
                error("연번 유효하지 않음")
                return null
            }
            val dayOfWeek = DayOfWeek.fromText(data[1])
            if(dayOfWeek == null){
                error("요일 유효하지 않음")
                return null
            }
            val duration = data[2].toDurationIntRange()
            if(duration.isEmpty()) {
                error("교시 유효하지 않음")
                return null
            }
            val name = data[3]
            val partType = LecturePartType.fromName(data[4])
            if(partType == null){
                error("분반 유효하지 않음")
                return null
            }
            val professorName = data[5]
            val isEssential = data[6] == "교필"
            val grades = data[7].toIntOrNull()
            if(grades == null){
                error("학점 유효하지 않음")
                return null
            }
            val times = data[8].toIntOrNull()
            if(times == null){
                error("시수 유효하지 않음")
                return null
            }
            val isPass = data[9] == "패스제"
            val type = LectureType.fromText(data[10])
            if(type == null) {
                error("수업구분 유효하지 않음")
                return null
            }
            val weekDuration = data[11].toIntRange()
            if(weekDuration == null || weekDuration.isEmpty()) {
                error("수업주차 유효하지 않음")
                return null
            }
            val isOnline = data[12] == "비대면"
            val numberOfPeople = data[13].toIntOrNull()
            if(numberOfPeople == null){
                error("인원 유효하지 않음")
                return null
            }
            val place = data[14]
            return Lecture(dayOfWeek, duration, name, partType, professorName, isEssential, grades, times, isPass, type, weekDuration, isOnline, numberOfPeople, place, null)
        }
    }
}