package lv.zarin.timekeep.data

import lv.zarin.timekeep.data.db.Converters
import lv.zarin.timekeep.data.db.RunStateType
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun knownNamesRoundTrip() {
        RunStateType.entries.forEach { assertEquals(it, converters.toRunStateType(converters.fromRunStateType(it))) }
        PictureId.entries.forEach { assertEquals(it, converters.toPictureId(converters.fromPictureId(it))) }
        SandColor.entries.forEach { assertEquals(it, converters.toSandColor(converters.fromSandColor(it))) }
    }

    @Test
    fun unknownNamesFallBack() {
        assertEquals(RunStateType.FINISHED, converters.toRunStateType("???"))
        assertEquals(PictureId.HEART, converters.toPictureId("???"))
        assertEquals(SandColor.LAVENDER, converters.toSandColor("???"))
    }
}
