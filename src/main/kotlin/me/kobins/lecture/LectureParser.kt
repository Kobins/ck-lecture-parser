package me.kobins.lecture

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.awt.Color
import java.io.File
import java.lang.Exception

object LectureParser {
    val leftText = listOf("과목명", "교수명", "강의실")

    private fun getFile(defaultPath: String): File {
        val defaultFile = File(defaultPath)
        if(defaultFile.isFile && defaultFile.exists() && defaultFile.canRead()) {
            return defaultFile
        }
        while(true) {
            println("교양 과목을 변환할 텍스트 파일 이름을 입력해 주세요(예: test.txt).")
            print("파일 경로 입력: ")
            val path = readLine() ?: continue
            val file = File(path)
            if(file.isFile && file.exists() && file.canRead()) {
                return file
            }
            println("${path}는 유효한 파일 경로가 아닙니다.")
        }
    }

    private fun generateFilter(type: String, context: String): (Lecture) -> Boolean {
        return when(type) {
            "과목" -> { lecture -> context in lecture.name }
            "교수" -> { lecture -> context in lecture.professorName }
            "구분" -> { lecture -> if(lecture.isEssential) context == "교필" else context == "교선" }
            "수업유형" -> { lecture -> if(lecture.isOnline) context == "비대면" else context == "대면" }
            else -> error("유효하지 않은 종류")
        }
    }
    private fun getFilters(fileName: String): List<Pair<String, LectureFilter>> {
        val file = File(fileName)
        if(!file.exists() || !file.isFile || !file.canRead()){
            return emptyList()
        }

        return buildList {
            val lines = file.readLines()
            for(line in lines) {
                if(line.startsWith("#")) continue
                if(!line.contains('=')) continue
                val split = line.split('=').map { it.trim() }
                if(split.size < 2) {
                    println("필터 ${line} 변환 실패: 인자 갯수 부족")
                    continue
                }
                val type = split[0]
                val context = split.subList(1, split.size).joinToString(" ")
                val filter = try {
                    generateFilter(type, context)
                }catch (e: Exception) {
                    println("필터 ${line} 변환 실패: ${e.message}")
                    continue
                }
                add("$type=$context" to filter)
            }
        }
    }

    val optionList = listOf(
        Option.builder("file")
            .hasArg()
            .argName("file")
            .desc("파일 경로를 정합니다. 기본 lecture.txt입니다.")
            .build(),
        Option.builder("maxduration")
            .hasArg()
            .argName("maxduration")
            .desc("최대 교시 수를 정합니다(10교시, 12교시, 13교시 ...). 기본 12교시입니다.")
            .build(),
    )
    @JvmStatic
    fun main(args: Array<String>) {
        println("=========================")
        println("교양 시간표 변환기")
        println("2023-1 기준 작동")
        println("만든이: https://github.com/Kobins")
        println("=========================")
        println()

        val options = Options().apply {
            optionList.forEach { addOption(it) }
        }
        val parser = DefaultParser()
        val line = try {
            parser.parse(options, args)
        }catch (e: ParseException) {
            println("오류 발생: ${e.message}")

            HelpFormatter().printHelp("lecture-parser", options)
            return
        }

        val path = line.getOptionValue("file")
        val maxDuration = line.getOptionValue("maxduration")?.toIntOrNull()?.takeIf { it in 10 .. 13 }
            ?: error("최대 교시 수가 유효하지 않습니다.")
        println("최대 교시 수: ${maxDuration}교시")

        val file = getFile(path)
        val blacklist = getFilters("blacklist.txt")
        println("다음 ${blacklist.size}개의 블랙리스트(blacklist.txt)를 적용함:")
        for((info, _) in blacklist) {
            println("- ${info}")
        }

        val content = file.readLines()
        println("${content.size} 개의 과목 정보를 읽어옴, 변환 시작 ...")
        val lectures = content
            .mapNotNull { Lecture.parse(it) }
            .filterNot { lecture -> blacklist.any { (_, filter) -> filter.invoke(lecture) } }
        println("${lectures.size} 개의 과목 정보를 변환 성공, 출력 시작 ...")

        fun Color.toXSSF(): XSSFColor {
            return XSSFColor(ByteArray(3).apply {
                this[0] = red.toByte()
                this[1] = blue.toByte()
                this[2] = green.toByte()
            })
        }
        val lectureNames = lectures.map { it.name }.distinct().shuffled()
        val colorMap = lectureNames.mapIndexed { index, name ->
            name to Color.getHSBColor(index.toFloat() / lectureNames.size, 0.4f, 0.9f).toXSSF()
        }.toMap()

        // 모든 과목을 PartType(분반, X, Y, Z같은) 기준으로 groupBy
        val lecturesByPartType = lectures.groupBy { it.partType }
        // 무슨 생각으로 그랬는지, 교양 시간표는 분반이 같고 시간표도 겹치는 과목이 많음
        // 분반이 리케이온 전체 공유가 아니라 단일 과목 기준으로 분배되서 그런가 ..?
        // 따라서, 같은 분반이어도 여러 행을 가지므로, 리스트가 2차원임 ㅋㅋ!
        val lectureMap = HashMap<LecturePartType, MutableList<MutableList<Lecture>>>()
        for((partType, lectureList) in lecturesByPartType) {
            val listByPartType = lectureMap.getOrPut(partType) { ArrayList() }
            for(insertingLecture in lectureList) {
                var inserted = false
                // 분반별 리스트 -> 분반 내 행 리스트를 순회함
                for(listByRow in listByPartType) {
                    // 같은 행 내에서, 시간표상 겹치는 과목이 없으면
                    if(listByRow
                        // 같은 행에 존재하는 모든 과목이
                        .filter { it.dayOfWeek == insertingLecture.dayOfWeek }
                        // 안 겹치면 삽입
                        .all { it.duration.isNotCrossing(insertingLecture.duration) }
                    ) {
                        // 그 행에 삽입 !!
                        inserted = true
                        listByRow.add(insertingLecture)
                        break
                    }
                }
                // 삽입 성공했으면 다음 과목으로 넘어감
                if(inserted) continue
                // 겹치는 게 있어 지금 존재하는 모든 행에서 실패한 경우에는 새로운 행을 만들어서 추가
                val list = ArrayList<Lecture>()
                list.add(insertingLecture)
                listByPartType.add(list)
            }
        }

        // xlsx 출력을 위해 apache poi 라이브러리 사용
        val workbook = XSSFWorkbook()
        workbook.missingCellPolicy = Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
        val sheet = workbook.createSheet("generated")
        sheet.defaultColumnWidth = 11

        val leftStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }
        val onlineLectureFont = workbook.createFont().apply {
            color = IndexedColors.RED.index
            bold = true
        }
        var rownum = 0
        for((partType, lectureRowList) in lectureMap.toSortedMap(Comparator.comparingInt { it.ordinal })) {
            // 행 갯수
            val amount = lectureRowList.size
            // 행 생성 (과목 행당 3줄(과목명, 교수명, 강의실))
            val rows = List<Row>(amount * 3) { index ->
                sheet.createRow(rownum + index).apply {
                    getCell(1).apply {
                        setCellValue(leftText[index % 3])
                        cellStyle = leftStyle
                    }
                }
            }
            // 분반 합치기
            sheet.addMergedRegion(CellRangeAddress(rownum, rownum + amount * 3 - 1, 0, 0))
            rows[0].getCell(0).apply {
                cellStyle = leftStyle
                setCellValue("${partType}반")
            }
            rownum += amount * 3
            for((rowIndex, lecturesInRow) in lectureRowList.withIndex()) {
                val nameRow         = rows[rowIndex * 3 + 0]
                val professorRow    = rows[rowIndex * 3 + 1]
                val placeRow        = rows[rowIndex * 3 + 2]
                for(lecture in lecturesInRow) {
                    // 맨 앞 2열 넘기고, 요일 오프셋 적용(월화수목금)
                    val dayOfWeekOffset = 2 + lecture.dayOfWeek.ordinal * maxDuration
                    // 2022-2 변경: duration이 IntRange -> List<IntRange>, 물성의미학 6학점(1-3,5-7) 5주블록식 ㄷㄷ
                    for(duration in lecture.duration) {
                        val firstColumn = dayOfWeekOffset + duration.first - 1
                        val lastColumn = dayOfWeekOffset + duration.last - 1
                        val nameRegionIndex = sheet.addMergedRegion(CellRangeAddress(nameRow.rowNum, nameRow.rowNum, firstColumn, lastColumn))
                        val professorRegionIndex = sheet.addMergedRegion(CellRangeAddress(professorRow.rowNum, professorRow.rowNum, firstColumn, lastColumn))
                        val placeRegionIndex = sheet.addMergedRegion(CellRangeAddress(placeRow.rowNum, placeRow.rowNum, firstColumn, lastColumn))
                        val color = colorMap[lecture.name]
                        if(color == null) {
                            println("예외 발생: ${lecture.name}에 대한 색상 없음")
                            continue
                        }

                        // 각 열에 전부 스타일 적용해야 함
                        for(column in firstColumn .. lastColumn) {
                            nameRow.getCell(column).apply {
                                cellStyle = workbook.createCellStyle().apply {
                                    borderLeft = BorderStyle.MEDIUM
                                    borderTop = BorderStyle.MEDIUM
                                    borderBottom = BorderStyle.THIN
                                    borderRight = BorderStyle.MEDIUM
                                    setFillForegroundColor(color)
                                    fillPattern = FillPatternType.SOLID_FOREGROUND
                                    alignment = HorizontalAlignment.CENTER
                                    verticalAlignment = VerticalAlignment.CENTER
                                }
                                if(column == firstColumn) {
                                    val essential =
                                        if(lecture.isEssential) "(必)"
                                        else ""
                                    setCellValue("${lecture.name}${essential}")
                                }
                            }
                            professorRow.getCell(column).apply {
                                cellStyle = workbook.createCellStyle().apply {
                                    borderLeft = BorderStyle.MEDIUM
                                    borderTop = BorderStyle.THIN
                                    borderBottom = BorderStyle.THIN
                                    borderRight = BorderStyle.MEDIUM
                                    setFillForegroundColor(color)
                                    fillPattern = FillPatternType.SOLID_FOREGROUND
                                    alignment = HorizontalAlignment.CENTER
                                    verticalAlignment = VerticalAlignment.CENTER
                                }
                                if(column == firstColumn) {
                                    val type =
                                        if (!lecture.isOnline) "대면"
                                        else if (lecture.note != LectureNote.OFFLINE_POSSIBILITY_AFTER_6WEEKS) "비대면"
                                        else "혼합"
                                    setCellValue("${lecture.professorName}(${type})")
                                }
                            }
                            placeRow.getCell(column).apply {
                                cellStyle = workbook.createCellStyle().apply {
                                    borderLeft = BorderStyle.MEDIUM
                                    borderTop = BorderStyle.THIN
                                    borderBottom = BorderStyle.MEDIUM
                                    borderRight = BorderStyle.MEDIUM
                                    setFillForegroundColor(color)
                                    fillPattern = FillPatternType.SOLID_FOREGROUND
                                    alignment = HorizontalAlignment.CENTER
                                    verticalAlignment = VerticalAlignment.CENTER
                                    // 비대면 수업 빨간 칠
                                    if(lecture.isOnline) {
                                        setFont(onlineLectureFont)
                                    }
                                }
                                if(column == firstColumn) setCellValue("${lecture.place}")
                            }
                        }
                    }
                }
            }
        }
        val now = System.currentTimeMillis()
        val savingFile = File("${file.nameWithoutExtension}-${now}.xlsx")
        val outputStream = savingFile.outputStream()
        workbook.write(outputStream)
        outputStream.close()
        println("${savingFile.name} 에 출력 완료!")
    }
}

