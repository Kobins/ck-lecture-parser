package me.kobins.lecture


fun String.toDurationIntRange(rangeBy: String = "-", divideBy: String = ","): List<IntRange> {
    if(!this.contains(rangeBy)) return emptyList()
    val divide = split(divideBy)
    if(divide.isEmpty()) return emptyList()
    return divide.mapNotNull { it.toIntRange(rangeBy) }
}

fun String.toIntRange(splitBy: String = "-"): IntRange? {
    if(!this.contains(splitBy)) return null
    val split = split(splitBy)
    if(split.size != 2) return null
    val first = split[0].toIntOrNull() ?: return null
    val second = split[1].toIntOrNull() ?: return null
    return first.rangeTo(second)
}

fun IntRange.isNotCrossing(other: IntRange): Boolean {
    return (this.first < other.first && this.last < other.first)
            || (other.first < this.first && other.last < this.first)
}

fun List<IntRange>.isNotCrossing(other: List<IntRange>): Boolean {
    return all { r1 -> other.all { r2 -> r1.isNotCrossing(r2) } }
}

fun printlnWarn(message: String) = println("[WARN]: $message")