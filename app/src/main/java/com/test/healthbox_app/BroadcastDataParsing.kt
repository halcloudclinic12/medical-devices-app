package com.test.healthbox_app

class BroadcastDataParsing(private var onBroadcastDataParsing: OnBroadcastDataParsing?) {
    private val mOldNumberId = -1
    fun dataParsing(data: ByteArray?) {

        if (data == null) {
            return
        }

        if (data.size >= 15) {
            val deviceType = ((data[8].toInt() shr 5) and 0x01)

            val weightStatus = (data[8].toInt() and 0x01)

            val numberId = data[1].toInt() and 0xff

            val weight = ((data[2].toInt() and 0xff) shl 8) or (data[3].toInt() and 0xff)

            val dataDecimal = (data[8].toInt() and 0x06)

            var weightDecimal = 1

            if (dataDecimal == 2) {
                weightDecimal = 0
            } else if (dataDecimal == 4) {
                weightDecimal = 2
            }

            var weightUnit = 0

            //单位
            val dataUnit = (data[8].toInt() and 0x18) shr 3

            if (dataUnit == 0) {
                weightUnit = 0
            } else if (dataUnit == 1) {
                weightUnit = 1
            } else if (dataUnit == 2) {
                weightUnit = 6
            } else if (dataUnit == 3) {
                weightUnit = 4
            }

            val adc = ((data[4].toInt() and 0xff) shl 8) + (data[5].toInt() and 0xff)

            val algorithmId = 1

            if (onBroadcastDataParsing != null) {
                onBroadcastDataParsing!!.getWeightData(
                    numberId, deviceType, weightUnit, weightDecimal, weightStatus, 0, weight, adc, algorithmId
                )
            }

        }
    }


    interface OnBroadcastDataParsing {
        /**
         * 获取重量数据
         * Weight data (Stabilize weight)
         *
         * @param deviceType     0x00 : weighing scale
         * 0x01: body fat scale
         * @param weightUnit     weight unit
         * @param weightDecimal  weight decimal point
         * @param weightStatus   0: real-time weight, 1: stable weight
         * @param weightNegative 0: positive weight; 1: negative weight
         * @param weight         raw data (Raw data)
         * @param adc            impedance 65535 indicates failure to measure impedance
         * @param algorithmId    algorithm id
         * @param numberId       dataId
         */
        fun getWeightData(
            numberId: Int,
            deviceType: Int,
            weightUnit: Int,
            weightDecimal: Int,
            weightStatus: Int,
            weightNegative: Int,
            weight: Int,
            adc: Int,
            algorithmId: Int
        )
    }
}