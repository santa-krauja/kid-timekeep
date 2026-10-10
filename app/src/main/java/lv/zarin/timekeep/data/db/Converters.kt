package lv.zarin.timekeep.data.db

import androidx.room.TypeConverter
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor

class Converters {
    @TypeConverter
    fun fromRunStateType(value: RunStateType): String = value.name

    @TypeConverter
    fun toRunStateType(name: String): RunStateType = RunStateType.entries.firstOrNull { it.name == name } ?: RunStateType.FINISHED

    @TypeConverter
    fun fromPictureId(value: PictureId): String = value.name

    @TypeConverter
    fun toPictureId(name: String): PictureId = PictureId.entries.firstOrNull { it.name == name } ?: PictureId.HEART

    @TypeConverter
    fun fromSandColor(value: SandColor): String = value.name

    @TypeConverter
    fun toSandColor(name: String): SandColor = SandColor.entries.firstOrNull { it.name == name } ?: SandColor.LAVENDER
}
