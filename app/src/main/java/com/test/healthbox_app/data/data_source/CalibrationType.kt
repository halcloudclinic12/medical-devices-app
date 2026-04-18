package com.test.healthbox_app.data.data_source

enum class CalibrationType {
    HEIGHT,
    TEMPERATURE
}

enum class CalibrationOperators(val symbol: String) {
    PLUS("+"),
    MINUS("-");

    override fun toString(): String = symbol

    companion object {
        fun fromSymbol(symbol: String): CalibrationOperators? {
            return values().find { it.symbol == symbol }
        }
    }
}