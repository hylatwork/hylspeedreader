package eu.hylatwork.hylspeedreader.data.epub.portable

/**
 * Read-only ZIP archive over an in-memory byte array.
 *
 * Reads the end-of-central-directory record, walks the central directory (the
 * authoritative list of entries), and decompresses an entry on demand via
 * [Inflate] (stored and deflate methods). Pure Kotlin, so it is the ZIP layer
 * of the iOS EPUB codec and is exercised by JVM unit tests.
 */
internal class PortableZip(private val bytes: ByteArray) {

    internal data class Entry(
        val name: String,
        val method: Int,
        val compressedSize: Int,
        val uncompressedSize: Int,
        val localHeaderOffset: Int
    )

    private val entries = LinkedHashMap<String, Entry>()
    private val byLowerCaseName = HashMap<String, Entry>()

    init {
        readCentralDirectory()
    }

    fun entry(name: String): Entry? = entries[name] ?: byLowerCaseName[name.lowercase()]

    fun read(name: String): ByteArray? = entry(name)?.let(::read)

    fun read(entry: Entry): ByteArray {
        val offset = entry.localHeaderOffset
        if (offset + 30 > bytes.size || u32(offset) != LOCAL_HEADER_SIGNATURE) {
            throw EpubFormatException("bad local header for '${entry.name}'")
        }
        val nameLength = u16(offset + 26)
        val extraLength = u16(offset + 28)
        val dataStart = offset + 30 + nameLength + extraLength
        if (dataStart + entry.compressedSize > bytes.size) {
            throw EpubFormatException("truncated entry '${entry.name}'")
        }
        val raw = bytes.copyOfRange(dataStart, dataStart + entry.compressedSize)
        return when (entry.method) {
            METHOD_STORED -> raw
            METHOD_DEFLATED -> Inflate.inflateRaw(raw)
            else -> throw EpubFormatException("unsupported compression method ${entry.method}")
        }
    }

    private fun readCentralDirectory() {
        val end = findEndOfCentralDirectory()
        val entryCount = u16(end + 10)
        var offset = u32(end + 16).toInt()
        repeat(entryCount) {
            if (u32(offset) != CENTRAL_HEADER_SIGNATURE) {
                throw EpubFormatException("bad central-directory record at $offset")
            }
            val method = u16(offset + 10)
            val compressedSize = u32(offset + 20).toInt()
            val uncompressedSize = u32(offset + 24).toInt()
            val nameLength = u16(offset + 28)
            val extraLength = u16(offset + 30)
            val commentLength = u16(offset + 32)
            val localHeaderOffset = u32(offset + 42).toInt()
            val name = bytes.copyOfRange(offset + 46, offset + 46 + nameLength).decodeToString()
            val entry = Entry(name, method, compressedSize, uncompressedSize, localHeaderOffset)
            entries[name] = entry
            byLowerCaseName[name.lowercase()] = entry
            offset += 46 + nameLength + extraLength + commentLength
        }
    }

    private fun findEndOfCentralDirectory(): Int {
        val minimum = maxOf(0, bytes.size - EOCD_MAX_SEARCH - 22)
        for (offset in bytes.size - 22 downTo minimum) {
            if (u32(offset) == EOCD_SIGNATURE) return offset
        }
        throw EpubFormatException("not a ZIP archive (no end-of-central-directory record)")
    }

    private fun u16(offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(offset: Int): Long {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return (bytes[offset].toInt() and 0xFF).toLong() or
            ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF).toLong() shl 24)
    }

    private companion object {
        const val LOCAL_HEADER_SIGNATURE = 0x04034b50L
        const val CENTRAL_HEADER_SIGNATURE = 0x02014b50L
        const val EOCD_SIGNATURE = 0x06054b50L
        const val EOCD_MAX_SEARCH = 0xFFFF
        const val METHOD_STORED = 0
        const val METHOD_DEFLATED = 8
    }
}
