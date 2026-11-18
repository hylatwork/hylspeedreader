package eu.hylatwork.hylspeedreader.domain

import eu.hylatwork.hylspeedreader.domain.model.ReadingPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the persistence payload: a blank/invalid stored position must be
 * dropped (so a corrupt preference file cannot drive a bogus restore), while
 * negative indices are clamped rather than discarded.
 */
class ReadingPositionTest {

    @Test
    fun `a blank book uri is discarded`() {
        assertNull(ReadingPosition("   ", 0, 0, 350).validOrNull())
    }

    @Test
    fun `negative values are clamped, not discarded`() {
        assertEquals(
            ReadingPosition("content://book", 0, 0, 0),
            ReadingPosition("content://book", -3, -9, -100).validOrNull()
        )
    }

    @Test
    fun `a normal position is returned unchanged`() {
        val position = ReadingPosition("content://book", 2, 40, 450)
        assertEquals(position, position.validOrNull())
    }
}
