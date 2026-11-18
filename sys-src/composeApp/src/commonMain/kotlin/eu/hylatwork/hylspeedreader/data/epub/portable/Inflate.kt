package eu.hylatwork.hylspeedreader.data.epub.portable

/**
 * Minimal, dependency-free DEFLATE (RFC 1951) decompressor for raw ZIP entry
 * streams.
 *
 * Pure Kotlin so it compiles for every KMP target (notably iOS, which has no
 * `java.util.zip`) and can be verified by plain JVM unit tests against streams
 * produced by `java.util.zip.Deflater`. Supports stored, fixed-Huffman and
 * dynamic-Huffman blocks, which is the full set an EPUB may contain.
 */
internal object Inflate {

    private val LENGTH_BASE = intArrayOf(
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258
    )
    private val LENGTH_EXTRA = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
    )
    private val DIST_BASE = intArrayOf(
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
    )
    private val DIST_EXTRA = intArrayOf(
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
    )

    private val CODE_LENGTH_ORDER =
        intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    private val FIXED_LITERAL = Huffman.build(IntArray(288) {
        when {
            it < 144 -> 8
            it < 256 -> 9
            it < 280 -> 7
            else -> 8
        }
    })
    private val FIXED_DISTANCE = Huffman.build(IntArray(30) { 5 })

    fun inflateRaw(input: ByteArray): ByteArray {
        val reader = BitReader(input)
        val sink = ByteSink()
        var last = false
        while (!last) {
            last = reader.bit() == 1
            when (reader.bits(2)) {
                0 -> stored(reader, sink)
                1 -> block(reader, sink, FIXED_LITERAL, FIXED_DISTANCE)
                2 -> {
                    val (literal, distance) = dynamic(reader)
                    block(reader, sink, literal, distance)
                }
                else -> throw EpubFormatException("invalid DEFLATE block type")
            }
        }
        return sink.result()
    }

    private fun stored(reader: BitReader, sink: ByteSink) {
        reader.alignToByte()
        val header = reader.bytes(4)
        val length = (header[0].toInt() and 0xFF) or ((header[1].toInt() and 0xFF) shl 8)
        val complement = (header[2].toInt() and 0xFF) or ((header[3].toInt() and 0xFF) shl 8)
        if (((length + complement) and 0xFFFF) != 0xFFFF) {
            throw EpubFormatException("corrupt stored-block length")
        }
        sink.append(reader.bytes(length))
    }

    private fun block(reader: BitReader, sink: ByteSink, literal: Huffman, distance: Huffman) {
        while (true) {
            val symbol = literal.decode(reader)
            when {
                symbol < 256 -> sink.put(symbol)
                symbol == 256 -> return
                else -> {
                    val lengthIndex = symbol - 257
                    if (lengthIndex >= LENGTH_BASE.size) {
                        throw EpubFormatException("invalid length code $symbol")
                    }
                    val length = LENGTH_BASE[lengthIndex] + reader.bits(LENGTH_EXTRA[lengthIndex])
                    val distanceSymbol = distance.decode(reader)
                    if (distanceSymbol >= DIST_BASE.size) {
                        throw EpubFormatException("invalid distance code")
                    }
                    val back = DIST_BASE[distanceSymbol] + reader.bits(DIST_EXTRA[distanceSymbol])
                    if (back > sink.size) throw EpubFormatException("distance $back exceeds output")
                    repeat(length) { sink.put(sink.byteAt(sink.size - back)) }
                }
            }
        }
    }

    private fun dynamic(reader: BitReader): Pair<Huffman, Huffman> {
        val literalCount = reader.bits(5) + 257
        val distanceCount = reader.bits(5) + 1
        val codeLengthCount = reader.bits(4) + 4

        val codeLengths = IntArray(19)
        for (i in 0 until codeLengthCount) codeLengths[CODE_LENGTH_ORDER[i]] = reader.bits(3)
        val codeLengthHuffman = Huffman.build(codeLengths)

        val lengths = IntArray(literalCount + distanceCount)
        var i = 0
        while (i < lengths.size) {
            when (val symbol = codeLengthHuffman.decode(reader)) {
                in 0..15 -> lengths[i++] = symbol
                16 -> {
                    if (i == 0) throw EpubFormatException("length repeat with no previous symbol")
                    val previous = lengths[i - 1]
                    var count = 3 + reader.bits(2)
                    while (count-- > 0 && i < lengths.size) lengths[i++] = previous
                }
                17 -> {
                    var count = 3 + reader.bits(3)
                    while (count-- > 0 && i < lengths.size) lengths[i++] = 0
                }
                18 -> {
                    var count = 11 + reader.bits(7)
                    while (count-- > 0 && i < lengths.size) lengths[i++] = 0
                }
                else -> throw EpubFormatException("invalid code-length symbol $symbol")
            }
        }
        return Huffman.build(lengths.head(literalCount)) to Huffman.build(lengths.tail(literalCount))
    }

    private fun IntArray.head(count: Int): IntArray = IntArray(count) { this[it] }
    private fun IntArray.tail(from: Int): IntArray = IntArray(size - from) { this[from + it] }

    private const val MAX_BITS = 15

    /** Canonical Huffman decode table, built once per block. */
    private class Huffman(private val counts: IntArray, private val symbols: IntArray) {

        fun decode(reader: BitReader): Int {
            var code = 0
            var first = 0
            var index = 0
            for (length in 1..MAX_BITS) {
                code = code or reader.bit()
                val count = counts[length]
                if (code - count < first) return symbols[index + (code - first)]
                index += count
                first = (first + count) shl 1
                code = code shl 1
            }
            throw EpubFormatException("invalid Huffman code")
        }

        companion object {
            fun build(lengths: IntArray): Huffman {
                val counts = IntArray(MAX_BITS + 1)
                for (length in lengths) {
                    if (length < 0 || length > MAX_BITS) {
                        throw EpubFormatException("invalid code length $length")
                    }
                    counts[length]++
                }
                counts[0] = 0
                val offsets = IntArray(MAX_BITS + 2)
                var total = 0
                for (i in 1..MAX_BITS) {
                    offsets[i] = total
                    total += counts[i]
                }
                val symbols = IntArray(total)
                for (i in lengths.indices) {
                    val length = lengths[i]
                    if (length != 0) symbols[offsets[length]++] = i
                }
                return Huffman(counts, symbols)
            }
        }
    }

    /** LSB-first bit reader, as required by the DEFLATE bit ordering. */
    private class BitReader(private val data: ByteArray) {

        private var position = 0
        private var bitBuffer = 0
        private var bitCount = 0

        fun bit(): Int {
            if (bitCount == 0) {
                if (position >= data.size) {
                    throw EpubFormatException("unexpected end of compressed stream")
                }
                bitBuffer = data[position++].toInt() and 0xFF
                bitCount = 8
            }
            val value = bitBuffer and 1
            bitBuffer = bitBuffer ushr 1
            bitCount--
            return value
        }

        fun bits(count: Int): Int {
            var result = 0
            for (i in 0 until count) result = result or (bit() shl i)
            return result
        }

        /** Discards the remaining bits of the current partial byte. */
        fun alignToByte() {
            bitBuffer = 0
            bitCount = 0
        }

        fun bytes(count: Int): ByteArray {
            val out = ByteArray(count)
            var written = 0
            while (written < count && bitCount >= 8) {
                out[written++] = bitBuffer.toByte()
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
            }
            val remaining = count - written
            if (remaining > 0) {
                if (position + remaining > data.size) {
                    throw EpubFormatException("unexpected end of stored block")
                }
                data.copyInto(out, written, position, position + remaining)
                position += remaining
            }
            return out
        }
    }

    /** Growable output buffer shared by the blocks of one stream. */
    private class ByteSink(initial: Int = 1024) {

        private var buffer = ByteArray(initial.coerceAtLeast(16))

        var size = 0
            private set

        private fun ensure(extra: Int) {
            val needed = size + extra
            if (needed > buffer.size) {
                var next = buffer.size * 2
                while (next < needed) next *= 2
                buffer = buffer.copyOf(next)
            }
        }

        fun put(value: Int) {
            ensure(1)
            buffer[size++] = value.toByte()
        }

        fun byteAt(index: Int): Int = buffer[index].toInt() and 0xFF

        fun append(bytes: ByteArray) {
            ensure(bytes.size)
            bytes.copyInto(buffer, size, 0, bytes.size)
            size += bytes.size
        }

        fun result(): ByteArray = buffer.copyOf(size)
    }
}
