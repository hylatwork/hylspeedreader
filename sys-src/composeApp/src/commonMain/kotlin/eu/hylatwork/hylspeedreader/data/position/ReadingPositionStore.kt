package eu.hylatwork.hylspeedreader.data.position

import eu.hylatwork.hylspeedreader.domain.model.ReadingPosition

/**
 * Persists the last reading position so a reopened book resumes where the
 * reader left off, even across process death.
 *
 * A port rather than a concrete class so the UI layer depends on the
 * behaviour, not on Android storage (and so it can be faked in tests).
 */
interface ReadingPositionStore {

    /** Returns the last saved position, or null when there is nothing usable. */
    fun load(): ReadingPosition?

    fun save(position: ReadingPosition)

    fun clear()
}
