package lv.zarin.timekeep.domain.format

import org.junit.Test
import org.junit.Assert.assertEquals

class DurationFormatTest {
    @Test fun formats() {
        assertEquals("8:12", formatClock(492_000, true))
        assertEquals("0:45", formatClock(45_000, false))
        assertEquals("1:02:03", formatClock(3_723_000, false))
        assertEquals("0:01", formatClock(1, true))
        assertEquals("0:00", formatClock(999, false))
        assertEquals("0:00", formatClock(0, true))
        assertEquals("1:00:00", formatClock(3_600_000, true))
        assertEquals("0:00", formatClock(-5, true))
    }

    @Test fun splits() {
        assertEquals(DurationParts(1, 2, 4), splitDuration(3_723_001, true))
        assertEquals(DurationParts(1, 2, 3), splitDuration(3_723_999, false))
    }
}
