package com.test.healthbox_app

import com.test.healthbox_app.data.ble.A1cRecordParser
import com.test.healthbox_app.domain.model.A1cUnit
import com.test.healthbox_app.domain.model.Hba1cMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [A1cRecordParser].
 *
 * Two fixtures are used:
 *  - VENDOR_SAMPLE   the worked example printed in the GlucoA1c BLE Developer Manual.
 *                    It is a *glucose* record (ucUnitFlag = 2), so it validates the
 *                    struct offsets, little-endian decode and checksum rule.
 *  - HBA1C_SAMPLE    a synthetic NGSP % record built to the same spec, 6.4 %.
 */
class A1cRecordParserTest {

    private lateinit var parser: A1cRecordParser

    @Before
    fun setUp() {
        parser = A1cRecordParser()
    }

    companion object {

        /** From the vendor manual. Glucose 16.7222 mmol/L, 2017-05-01 12:02:04, checksum 0xFD. */
        private const val VENDOR_SAMPLE_HEX =
            "02004862 4131632d 43485331 37433930 30303100 a5a50000 " +
                "00006d00 00005525 6666de41 1cc78541 e1070501 0c020407 fd"

        /** Synthetic: NGSP %, 6.4 %, 26.5 C, record 110, 2026-07-28 10:15:30, checksum 0x49. */
        private const val HBA1C_SAMPLE_HEX =
            "00004862 4131632d 43485331 37433930 30303100 a5a50000 " +
                "00006e00 00005505 0000d441 cdcccc40 ea07071c 0a0f1e03 49"

        /**
         * Real captures of the undocumented ENVELOPE format from a physical
         * HbA1c-EJB24510443 meter (2026-09-13), each confirmed against the number
         * the meter's own screen displayed for that test:
         *  - ENVELOPE_A: decodes to 6.47..., meter screen showed 6.5
         *  - ENVELOPE_B: decodes to 6.39..., meter screen showed 6.4
         * Captured as delivered — each string is one BLE notification fragment, in
         * the exact sizes the meter actually sent, for the reassembly test below.
         */
        private val ENVELOPE_A_FRAGMENTS = listOf(
            "42 54 53 48 62 41 31 63 2D 45",
            "4A 42 32 34 35 31 30 34",
            "34 33 A5 FF 3E 01 01 03 A3 00 40 02",
            "99 0D 0F 30 1C 02 1C 22 7E",
            "03 21 30 CF 40 00 00 00 00 00 00",
            "00 00 45 4E 44"
        )

        private val ENVELOPE_B_FRAGMENTS = listOf(
            "42 54 53 48 62 41 31 63 2D 45 4A 42 32 34 35",
            "31 30 34 34 33 A5 FF 3E 01 14",
            "03 B7 00 58 02 99 0D 10 09 19",
            "03 1D 22 58 03 6C 91 CC 40 00 00 00 00 00 00 00 00 45 4E 44"
        )

        private fun hex(s: String): ByteArray {
            val clean = s.replace(" ", "").replace("\n", "")
            return ByteArray(clean.length / 2) {
                clean.substring(it * 2, it * 2 + 2).toInt(16).toByte()
            }
        }

        private fun epochOf(
            year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int
        ): Long = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
        }.timeInMillis
    }

    private val vendorSample get() = hex(VENDOR_SAMPLE_HEX)
    private val hba1cSample get() = hex(HBA1C_SAMPLE_HEX)

    // ── Fixture sanity ────────────────────────────────────────────────────────

    @Test
    fun `fixtures are 49 bytes`() {
        assertEquals(A1cRecordParser.RECORD_SIZE, vendorSample.size)
        assertEquals(A1cRecordParser.RECORD_SIZE, hba1cSample.size)
    }

    // ── Vendor sample: struct layout and endianness ───────────────────────────

    @Test
    fun `decodes vendor sample with correct offsets and little-endian fields`() {
        val m = parser.decode(vendorSample)
        assertNotNull("Vendor sample must decode (checksum 0xFD)", m)
        m!!

        assertEquals("HbA1c-CHS17C90001", m.meterId)
        assertEquals(109L, m.uniqueRecordNum)
        assertEquals(7, m.recordNumOfDay)
        assertEquals(27.7999, m.temperatureC, 0.001)
        assertEquals(16.7222, m.value, 0.001)
        assertEquals(A1cUnit.GLUCOSE_MMOL_L, m.unit)
        assertEquals(epochOf(2017, 5, 1, 12, 2, 4), m.deviceTimeMillis)
    }

    @Test
    fun `vendor sample is flagged as glucose and never valid as HbA1c`() {
        val m = parser.decode(vendorSample)!!
        assertTrue("ucUnitFlag 2 must be recognised as glucose", m.isGlucoseRecord)
        assertFalse("A glucose record must not pass as a valid HbA1c result", m.isValid)
        assertNull("No NGSP normalisation for glucose records", m.ngspPercent)
    }

    // ── HbA1c sample ──────────────────────────────────────────────────────────

    @Test
    fun `decodes HbA1c sample as a valid NGSP percent result`() {
        val m = parser.decode(hba1cSample)
        assertNotNull("HbA1c sample must decode (checksum 0x49)", m)
        m!!

        assertEquals(A1cUnit.NGSP_PERCENT, m.unit)
        assertEquals(6.4, m.value, 0.001)
        assertEquals(6.4, m.ngspPercent!!, 0.001)
        assertEquals(26.5, m.temperatureC, 0.001)
        assertEquals(110L, m.uniqueRecordNum)
        assertEquals(3, m.recordNumOfDay)
        assertEquals("HbA1c-CHS17C90001", m.meterId)
        assertEquals(epochOf(2026, 7, 28, 10, 15, 30), m.deviceTimeMillis)
        assertFalse(m.isGlucoseRecord)
        assertTrue(m.isValid)
    }

    @Test
    fun `derives eAG and IFCC from NGSP percent`() {
        val m = parser.decode(hba1cSample)!!
        // 28.7 * 6.4 - 46.7 = 136.98
        assertEquals(136.98, m.eagMgDl!!, 0.01)
        // (6.4 - 2.15) * 10.929 = 46.448
        assertEquals(46.448, m.ifccMmolMol!!, 0.01)
    }

    // ── Fragmentation / reassembly ────────────────────────────────────────────

    @Test
    fun `returns the record when the whole frame arrives in one notification`() {
        assertNotNull(parser.accept(hba1cSample))
    }

    @Test
    fun `reassembles a 20-20-9 fragment split`() {
        val f = hba1cSample
        assertNull("first fragment is incomplete", parser.accept(f.copyOfRange(0, 20)))
        assertNull("second fragment is still incomplete", parser.accept(f.copyOfRange(20, 40)))

        val m = parser.accept(f.copyOfRange(40, 49))
        assertNotNull("third fragment completes the frame", m)
        assertEquals(6.4, m!!.ngspPercent!!, 0.001)
    }

    @Test
    fun `reassembles when delivered one byte at a time`() {
        val f = hba1cSample
        for (i in 0 until f.size - 1) {
            assertNull("byte $i should not complete the frame", parser.accept(byteArrayOf(f[i])))
        }
        val m = parser.accept(byteArrayOf(f[f.size - 1]))
        assertNotNull("final byte completes the frame", m)
        assertEquals(6.4, m!!.ngspPercent!!, 0.001)
    }

    @Test
    fun `handles two records concatenated in a single chunk`() {
        val combined = hba1cSample + vendorSample
        val m = parser.accept(combined)
        // Both frames are consumed; the last decoded one is surfaced.
        assertNotNull(m)
        assertTrue("second frame is the vendor glucose record", m!!.isGlucoseRecord)

        // Buffer must be empty afterwards — a fresh frame still decodes cleanly.
        val next = parser.accept(hba1cSample)
        assertNotNull(next)
        assertEquals(6.4, next!!.ngspPercent!!, 0.001)
    }

    @Test
    fun `discards a stalled partial frame instead of splicing it onto the next record`() {
        val f = hba1cSample
        val t0 = 1_000_000L

        assertNull(parser.accept(f.copyOfRange(0, 20), nowMillis = t0))

        // 6 s later the rest of a *new* record arrives; the stale 20 bytes must be dropped.
        val late = t0 + 6_000L
        assertNull(parser.accept(f.copyOfRange(0, 20), nowMillis = late))
        assertNull(parser.accept(f.copyOfRange(20, 40), nowMillis = late))
        val m = parser.accept(f.copyOfRange(40, 49), nowMillis = late)

        assertNotNull("the new frame must decode correctly", m)
        assertEquals(6.4, m!!.ngspPercent!!, 0.001)
    }

    @Test
    fun `reset clears a partial frame`() {
        assertNull(parser.accept(hba1cSample.copyOfRange(0, 30)))
        parser.reset()
        assertNull(parser.accept(hba1cSample.copyOfRange(0, 20)))
        assertNull(parser.accept(hba1cSample.copyOfRange(20, 40)))
        assertNotNull(parser.accept(hba1cSample.copyOfRange(40, 49)))
    }

    // ── Corrupt / hostile input ───────────────────────────────────────────────

    @Test
    fun `rejects a frame with a bad checksum`() {
        val corrupt = hba1cSample.copyOf()
        corrupt[48] = (corrupt[48] + 1).toByte()
        assertNull(parser.decode(corrupt))
    }

    @Test
    fun `rejects a frame whose payload was altered`() {
        val corrupt = hba1cSample.copyOf()
        corrupt[36] = (corrupt[36].toInt() xor 0xFF).toByte()  // flip part of dRecordValue
        assertNull("checksum must catch payload corruption", parser.decode(corrupt))
    }

    @Test
    fun `rejects an unknown unit flag without throwing`() {
        val frame = hba1cSample.copyOf()
        frame[0] = 7
        // Re-checksum so it fails on the unit flag, not the checksum.
        var sum = 0
        for (i in 0 until 48) sum += (frame[i].toInt() and 0xFF)
        frame[48] = (sum and 0xFF).toByte()

        assertNull(parser.decode(frame))
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(parser.accept(ByteArray(0)))
    }

    @Test
    fun `returns null for a frame shorter than the record size`() {
        assertNull(parser.decode(hba1cSample.copyOfRange(0, 30)))
    }

    @Test
    fun `marks an out-of-range value invalid but still decodes it`() {
        val frame = hba1cSample.copyOf()
        // dRecordValue at offset 36 -> 25.0f little-endian = 00 00 C8 41
        frame[36] = 0x00; frame[37] = 0x00; frame[38] = 0xC8.toByte(); frame[39] = 0x41
        var sum = 0
        for (i in 0 until 48) sum += (frame[i].toInt() and 0xFF)
        frame[48] = (sum and 0xFF).toByte()

        val m = parser.decode(frame)
        assertNotNull(m)
        assertEquals(25.0, m!!.value, 0.001)
        assertFalse("25 % is outside the plausible range", m.isValid)
    }

    @Test
    fun `survives a long run of junk without leaking or throwing`() {
        repeat(50) {
            parser.accept(ByteArray(20) { i -> (i * 7).toByte() })
        }
        // A real frame must still decode afterwards.
        parser.reset()
        assertNotNull(parser.accept(hba1cSample))
    }

    @Test
    fun `zeroed timestamp fields yield zero device time rather than a bogus date`() {
        val frame = hba1cSample.copyOf()
        for (i in 40..46) frame[i] = 0                       // year..seconds = 0
        var sum = 0
        for (i in 0 until 48) sum += (frame[i].toInt() and 0xFF)
        frame[48] = (sum and 0xFF).toByte()

        val m = parser.decode(frame)
        assertNotNull(m)
        assertEquals(0L, m!!.deviceTimeMillis)
    }

    // ── ENVELOPE format (undocumented, confirmed against real meter screens) ────

    @Test
    fun `decodes real ENVELOPE capture A matching the meter's displayed 6_5`() {
        val frame = hex(ENVELOPE_A_FRAGMENTS.joinToString(" "))
        val m = parser.decodeEnvelope(frame)

        assertNotNull("ENVELOPE_A must decode (has the fixed marker and a result)", m)
        m!!
        assertEquals("HbA1c-EJB24510443", m.meterId)
        assertEquals(6.47, m.value, 0.01)
        assertEquals(6.47, m.ngspPercent!!, 0.01)
        assertEquals(A1cUnit.NGSP_PERCENT, m.unit)
        assertFalse(m.isGlucoseRecord)
        assertTrue("6.47 is within the meter's 4.0-14.0% range", m.isValid)
    }

    @Test
    fun `decodes real ENVELOPE capture B matching the meter's displayed 6_4`() {
        val frame = hex(ENVELOPE_B_FRAGMENTS.joinToString(" "))
        val m = parser.decodeEnvelope(frame)

        assertNotNull("ENVELOPE_B must decode (has the fixed marker and a result)", m)
        m!!
        assertEquals("HbA1c-EJB24510443", m.meterId)
        assertEquals(6.39, m.value, 0.01)
        assertTrue(m.isValid)
    }

    @Test
    fun `reassembles ENVELOPE_A from the exact fragments the meter sent`() {
        var latest: Hba1cMeasurement? = null
        ENVELOPE_A_FRAGMENTS.forEach { fragmentHex ->
            val result = parser.accept(hex(fragmentHex))
            if (result != null) latest = result
        }

        assertNotNull("the final fragment (carrying \"END\") must complete the frame", latest)
        assertEquals(6.47, latest!!.value, 0.01)
    }

    @Test
    fun `reassembles ENVELOPE_B from the exact fragments the meter sent`() {
        var latest: Hba1cMeasurement? = null
        ENVELOPE_B_FRAGMENTS.forEach { fragmentHex ->
            val result = parser.accept(hex(fragmentHex))
            if (result != null) latest = result
        }

        assertNotNull(latest)
        assertEquals(6.39, latest!!.value, 0.01)
    }

    @Test
    fun `accept routes a chunk starting with BTS to the envelope path, not the 49-byte struct path`() {
        // A naive implementation would try to slice this as a 49-byte STRUCT frame
        // once enough bytes accumulate; it must not — this is an ENVELOPE frame and
        // is only 55 bytes with no valid struct checksum anywhere in it.
        val combined = ENVELOPE_A_FRAGMENTS.joinToString("") { it.replace(" ", "") }
        val frame = hex(combined)
        assertNotNull(parser.accept(frame))
    }

    @Test
    fun `rejects an envelope missing the fixed marker`() {
        val frame = hex(ENVELOPE_A_FRAGMENTS.joinToString(" "))
        frame[20] = 0x00  // corrupt the first byte of the A5 FF 3E 01 marker
        assertNull(parser.decodeEnvelope(frame))
    }

    @Test
    fun `envelope reassembly does not corrupt a later real frame`() {
        // A stray notification that never resolves to "END" must not permanently wedge
        // the parser — the stale-frame timeout should clear it before a real frame
        // arrives, exactly as it does for the STRUCT format.
        val t0 = 1_000_000L
        parser.accept(hex("42 54 53 00 00"), nowMillis = t0)  // starts with "BTS", never completes

        val late = t0 + 6_000L
        var latest: Hba1cMeasurement? = null
        ENVELOPE_A_FRAGMENTS.forEach { fragmentHex ->
            val result = parser.accept(hex(fragmentHex), nowMillis = late)
            if (result != null) latest = result
        }

        assertNotNull("a fresh ENVELOPE frame must still decode after the stale one times out", latest)
        assertEquals(6.47, latest!!.value, 0.01)
    }
}
