package com.test.healthbox_app.data.repository

import android.util.Log
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.ble.A1cRecordParser
import com.test.healthbox_app.domain.model.BloodPressureMeasurement
import com.test.healthbox_app.domain.model.GlucoseMeasurement
import com.test.healthbox_app.domain.model.HeightMeasurement
import com.test.healthbox_app.domain.model.Measurement
import com.test.healthbox_app.domain.model.PulseMeasurement
import com.test.healthbox_app.domain.model.TemperatureMeasurement
import com.test.healthbox_app.domain.repository.MeasurementRepository
import com.test.healthbox_app.presentation.tests.bloodPressure.ByteHelper
import com.test.healthbox_app.presentation.util.StringUtils
import java.nio.charset.Charset
import javax.inject.Inject

class MeasurementRepositoryImpl @Inject constructor() : MeasurementRepository {

    var validMeasurement = PulseMeasurement(
        oxygenSaturation = 0, pulseRate = 0, pi = 0.0, isValid = false
    )

    private val packetBuffers = mutableMapOf<DeviceType, StringBuilder>()

    /**
     * A1cEZ 2.0 records arrive as ~3 fragments of a 49-byte binary frame, so the
     * parser is stateful and must be a single long-lived instance.
     */
    private val hba1cParser = A1cRecordParser { message ->
        Log.w("A1cRecordParser", message)
    }

    override fun parseMeasurement(deviceType: DeviceType, rawData: ByteArray): Measurement? {
        return when (deviceType) {
            DeviceType.HEIGHT -> parseHeightData(rawData)
            DeviceType.THERMOMETER -> parseTemperatureData(rawData)
            DeviceType.PULSE -> parsePulseData(rawData)
            DeviceType.BLOOD_PRESSURE_MONITOR -> parseBloodPressureData(rawData)
            DeviceType.GLUCOSE_METER -> parseGlucoseData(rawData)
            DeviceType.HBA1C_METER -> hba1cParser.accept(rawData)
            else -> null
        }
    }

    /**
     * Drops any partially received HbA1c frame. Call on connect and on disconnect so
     * a truncated record can never be spliced onto the next test's data.
     */
    override fun resetHba1cBuffer() = hba1cParser.reset()

    private fun parseHeightData(rawData: ByteArray): HeightMeasurement {
        val heightString = rawData.toString(Charset.defaultCharset()).trim()

        return try {
            // Example: Height device returns height in cm as a string like "175.5"

            val heightValue = heightString.toIntOrNull()

            HeightMeasurement(
                heightCm = heightValue ?: 0, isValid = heightValue != null && heightValue > 0f, error = heightString
            )

        } catch (e: Exception) {
            HeightMeasurement(heightCm = 0, isValid = false, error = heightString)
        }
    }

    private fun parseTemperatureData(rawData: ByteArray): TemperatureMeasurement {
        return try {
            // Example: Temperature device might return temperature as a string like "36.5"
//            val tempString = rawData.toString(Charset.defaultCharset()).trim()
//            val tempValue = tempString.toFloatOrNull()


            val a = java.lang.Byte.toUnsignedInt(rawData[2])
            val b = java.lang.Byte.toUnsignedInt(rawData[3])
            Log.e("temperatureDataLogs", " : a :$a  : b :$b")

            val temperatureCentigrade: Double = StringUtils.singleDecimalValue(((a shl 8) + b) * 1.0 / 100).toDouble()
            Log.e("temperatureDataLogs", " : Centigrade 1 :$temperatureCentigrade")

            /*temperatureCentigrade = Math.round(tem * 10) * 1.0 / 10
            Log.e("temperatureDataLogs", " : Centigrade :temperatureCentigrade")*/

            val fahrenheit = StringUtils.singleDecimalValue((temperatureCentigrade * 9 / 5 + 32)).toDouble()
            Log.e("temperatureDataLogs", " : fahrenheit :$fahrenheit")

            TemperatureMeasurement(
                temperatureCelsius = temperatureCentigrade ?: 0.0,
                temperatureFahrenheit = fahrenheit ?: 0.0,
                isValid = temperatureCentigrade in 30f..45f // Valid human temperature range
            )

        } catch (e: Exception) {
            TemperatureMeasurement(temperatureCelsius = 0.0, temperatureFahrenheit = 0.0, isValid = false)
        }
    }

    /*private fun parsePulseData(rawData: ByteArray): PulseMeasurement {
        return try {
            // Example: Pulse oximeter might return data in format "98,72" (SpO2,Pulse)
            val dataString = rawData.toString(Charset.defaultCharset()).trim()
            val parts = dataString.split(",")

//            val oxygenSaturation = parts.getOrNull(0)?.toIntOrNull() ?: 0
//            val pulseRate = parts.getOrNull(1)?.toIntOrNull() ?: 0

            var oxygenSaturation: Int = 0
            var pulseRate: Int = 0
            var pi: Int = 0

//            val data = rawData

            rawData.forEach {
                Log.e("BplOxyDatadata", "SPO2:$rawData  :  :  $it")

            }


            if (rawData[0].toInt() == -128) {

            } else if (rawData[0].toInt() == -127) {

                pulseRate = rawData[1].toInt() and 0xFF
                oxygenSaturation = rawData[2].toInt() and 0xFF
                pi = rawData[3].toInt() and 0xFF

                if (oxygenSaturation != 127 || oxygenSaturation != 0) {
                    Log.e("BplOxyDataLogs", " : SPO2 : " + oxygenSaturation + "% :  Pulse : " + pulseRate + " : PI : " + pi.toDouble() * 10 / 100)

                    Log.e("oxygen_res", " : SpO2 :" + oxygenSaturation)
                }
            }

            PulseMeasurement(
                oxygenSaturation = oxygenSaturation,
                pulseRate = pulseRate,
                pi = 0,
                isValid = oxygenSaturation in 80..100 && pulseRate in 40..200
            )

        } catch (e: Exception) {
            PulseMeasurement(oxygenSaturation = 0, pulseRate = 0, pi = 0, isValid = false)
        }
    }*/


    private fun parsePulseData(rawData: ByteArray): PulseMeasurement {
        return try {

            if (rawData[0].toInt() == -128) {
                // Handle this case if needed
                PulseMeasurement(
                    oxygenSaturation = 0, pulseRate = 0, pi = 0.0, isValid = false
                )
            } else if (rawData[0].toInt() == -127) {
                // Parse the data
                val parsedPulseRate = rawData[1].toInt() and 0xFF
                val parsedOxygenSaturation = rawData[2].toInt() and 0xFF
                val parsedPi = (rawData[3].toInt() and 0xFF).toDouble() * 10 / 100

                if (parsedOxygenSaturation != 127 && parsedOxygenSaturation != 0) {
                    Log.e("BplOxyDataLogs", " : SPO2 : $parsedOxygenSaturation% :  Pulse : $parsedPulseRate : PI : $parsedPi")
                    Log.e("oxygen_res", " : SpO2 :$parsedOxygenSaturation")


                    // Return immediately with the parsed values
                    val measurement = PulseMeasurement(
                        oxygenSaturation = parsedOxygenSaturation,
                        pulseRate = parsedPulseRate,
                        pi = parsedPi,
                        isValid = parsedOxygenSaturation in 80..100 && parsedPulseRate in 40..200
                    )

                    validMeasurement = measurement

                    return measurement
                }
            }

            return validMeasurement

        } catch (e: Exception) {
            PulseMeasurement(oxygenSaturation = 0, pulseRate = 0, pi = 0.0, isValid = false)
        }
    }

    private fun parseBloodPressureData(rawData: ByteArray): BloodPressureMeasurement {

        var bloodPressureMesurement = BloodPressureMeasurement(systolic = 0, diastolic = 0, pulseRate = 0, isTesting = false, isValid = false)

        return try {

            if (rawData.size == 1) {
//            mBluetoothGatt.writeData(startCmd);
            } else if (rawData.size == 7) {
//                                 BloodPressureResult(TESTING, "" + ByteHelper.unsignedByteToInt(data[4]))
                bloodPressureMesurement = BloodPressureMeasurement(
                    systolic = ByteHelper.unsignedByteToInt(rawData[4]), diastolic = 0, pulseRate = 0, isTesting = true, isValid = true
                )

            } else if (rawData.size == 8) {
                val systolic = ByteHelper.unsignedByteToInt(rawData[3])
                val diastolic = ByteHelper.unsignedByteToInt(rawData[4])
                val heartRate = ByteHelper.unsignedByteToInt(rawData[5])

                Log.d("MeasurementParsed", " Sys: $systolic  : dis :  $diastolic   : heartRate :  $heartRate")

                bloodPressureMesurement =
                    BloodPressureMeasurement(systolic = systolic, diastolic = diastolic, pulseRate = heartRate, isTesting = false, isValid = true)

            }
            return bloodPressureMesurement

        } catch (e: Exception) {
            BloodPressureMeasurement(systolic = 0, diastolic = 0, pulseRate = 0, isTesting = false, isValid = false)
        }
    }

    private fun parseGlucoseData(rawData: ByteArray): GlucoseMeasurement? {

        val buffer = packetBuffers.getOrPut(DeviceType.GLUCOSE_METER) { StringBuilder() }

        buffer.append(rawData.toString(Charsets.ISO_8859_1))

        val accumulated = buffer.toString()
        Log.d("GlucoseParser", "Buffer now: [${accumulated.replace("\r", "\\r").replace("\n", "\\n")}]")

        // Not yet a complete reading — wait for more packets
        if (!accumulated.contains("\r\n\r\n")) return null

        // One or more complete readings available
        val readings = accumulated.split("\r\n\r\n").filter { it.isNotBlank() }

        // Keep any incomplete trailing data after last terminator
        val lastTerminator = accumulated.lastIndexOf("\r\n\r\n")
        val remainder = accumulated.substring(lastTerminator + 4)
        buffer.clear()
        if (remainder.isNotBlank()) buffer.append(remainder)

        // Parse each block, return the last valid one
        var lastValidReading: GlucoseMeasurement? = null

        readings.forEach { block ->
            val parsed = parseGlucoseBlock(block.trim())
            if (parsed != null) {
                Log.d(
                    "GlucoseParser",
                    "✓ Parsed  Logs New : ${parsed.sugar} "
                    /*${parsed.glucoseMmol} mmol/L  " +
                            "${parsed.month}/${parsed.day} ${parsed.hour}:${"% 02d".format(parsed.minute)}*/
                )
                lastValidReading = parsed
            } else {
                Log.w("GlucoseParser", "Could not parse block: [$block]")
            }
        }

        return lastValidReading
    }

    // ─────────────────────────────────────────────────────────────────────────
// Parses a single complete block, e.g.:
//   "EAL:\r\nGLU: 127 mg/dL\r\n3-20 0:59"
// ─────────────────────────────────────────────────────────────────────────
    private fun parseGlucoseBlock(block: String): GlucoseMeasurement? {
        return try {
            val lines = block.split("\r\n").map { it.trim() }.filter { it.isNotEmpty() }
            Log.d("GlucoseParser", "Parsing lines: $lines")

            val glucoseLine = lines.firstOrNull { it.startsWith("GLU:") } ?: return null

            val glucoseMgDl = glucoseLine
                .removePrefix("GLU:")
                .trim()
                .split(" ")
                .firstOrNull()
                ?.toIntOrNull()
                ?: return null

            val glucoseMmol = Math.round(glucoseMgDl * 0.0555 * 10) / 10.0

            val dateTimeLine = lines.firstOrNull {
                it.matches(Regex("\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}"))
            }

            var month = 0;
            var day = 0;
            var hour = 0;
            var minute = 0

            dateTimeLine?.let {
                val parts = it.trim().split("\\s+".toRegex())
                if (parts.size == 2) {
                    val dateParts = parts[0].split("-")
                    val timeParts = parts[1].split(":")
                    if (dateParts.size == 2 && timeParts.size == 2) {
                        month = dateParts[0].toIntOrNull() ?: 0
                        day = dateParts[1].toIntOrNull() ?: 0
                        hour = timeParts[0].toIntOrNull() ?: 0
                        minute = timeParts[1].toIntOrNull() ?: 0
                    }
                }
            }

            GlucoseMeasurement(
                sugar = glucoseMgDl,
                isValid = glucoseMgDl > 0,
            )

        } catch (e: Exception) {
            Log.e("GlucoseParser", "Parse error: ${e.message} — block: [$block]")
            null
        }
    }


}