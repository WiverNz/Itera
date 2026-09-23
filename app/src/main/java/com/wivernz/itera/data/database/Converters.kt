package com.wivernz.itera.data.database
import androidx.room.TypeConverter
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.DayPart
import com.wivernz.itera.domain.model.Difficulty
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.TrainingDayStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
class Converters {
    @TypeConverter
    fun date(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun epochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun time(value: Int?): LocalTime? = value?.let {
        LocalTime.ofSecondOfDay(it * 60L)
    }

    @TypeConverter
    fun minutes(value: LocalTime?): Int? = value?.let {
        it.hour * 60 + it.minute
    }

    @TypeConverter
    fun instant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun millis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun activityState(value: String?): ActivityState? = value?.let(ActivityState::valueOf)

    @TypeConverter
    fun activityStateName(value: ActivityState?): String? = value?.name

    @TypeConverter
    fun activitySource(value: String?): ActivitySource? = value?.let(ActivitySource::valueOf)

    @TypeConverter
    fun activitySourceName(value: ActivitySource?): String? = value?.name

    @TypeConverter
    fun exerciseType(value: String?): ExerciseType? = value?.let(ExerciseType::valueOf)

    @TypeConverter
    fun exerciseTypeName(value: ExerciseType?): String? = value?.name

    @TypeConverter
    fun dayPart(value: String?): DayPart? = value?.let(DayPart::valueOf)

    @TypeConverter
    fun dayPartName(value: DayPart?): String? = value?.name

    @TypeConverter
    fun difficulty(value: String?): Difficulty? = value?.let(Difficulty::valueOf)

    @TypeConverter
    fun difficultyName(value: Difficulty?): String? = value?.name

    @TypeConverter
    fun trainingDayStatus(value: String?): TrainingDayStatus? =
        value?.let(TrainingDayStatus::valueOf)

    @TypeConverter
    fun trainingDayStatusName(value: TrainingDayStatus?): String? = value?.name

    @TypeConverter
    fun recallGrade(value: String?): RecallGrade? = value?.let(RecallGrade::valueOf)

    @TypeConverter
    fun recallGradeName(value: RecallGrade?): String? = value?.name

    @TypeConverter
    fun reviewState(value: String?): ReviewState? = value?.let(ReviewState::valueOf)

    @TypeConverter
    fun reviewStateName(value: ReviewState?): String? = value?.name
}
