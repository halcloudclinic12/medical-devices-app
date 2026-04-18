package com.test.healthbox_app.domain.use_cases

import android.util.Log
import com.test.healthbox_app.domain.model.WeighingScaleResult
import com.test.healthbox_app.domain.repository.WeighingScaleRepository
import javax.inject.Inject

class WeighingScaleUseCases @Inject constructor(private val repository: WeighingScaleRepository) {


    suspend fun initSDK(): WeighingScaleResult<Unit> {
        Log.e("initSDKLogs", "  ::  Called  ::")
        return repository.initializeScale()
    }

    suspend fun startScanning(): WeighingScaleResult<Unit> {
        return repository.startScanning()
    }


    /*suspend fun initializeScale(): WeighingScaleResult<Unit> {
        return repository.initializeScale()
    }

    suspend fun startScanning(timeoutMs: Long = 10000): WeighingScaleResult<Unit> {
        return repository.startScanning(timeoutMs)
    }

    suspend fun stopScanning(): WeighingScaleResult<Unit> {
        return repository.stopScanning()
    }

    fun getWeightData(): Flow<WeightData> {
        return repository.getWeightDataStream()
    }

    fun getScannedDevicesStream(): Flow<List<BleDevice>> {
        return repository.getScannedDevicesStream()
    }

    suspend fun calculateBodyFatData(
        sex: Int,
        age: Int,
        height: Int,
        weightData: WeightData
    ): WeighingScaleResult<Pair<BodyFatData, MoreFatData>> {
        return repository.calculateBodyFatData(sex = sex, age = age, height = height, weightData = weightData)
    }*/

    /*fun startWeighingProcess(
        sex: Int,
        age: Int,
        height: Int
    ): Flow<WeighingScaleResult<WeightMeasurement>> {
        return kotlinx.coroutines.flow.flow {
            emit(WeighingScaleResult.Loading)

            // Check permissions
            *//*if (!checkPermissionsUseCase()) {
                if (!requestPermissionsUseCase()) {
                    emit(WeighingScaleResult.Error(SecurityException("Permissions not granted")))
                    return@flow
                }
            }*//*

            // Initialize scale
            when (val initResult = initializeScale()) {


                is WeighingScaleResult.Error -> {
                    emit(WeighingScaleResult.Error(initResult.exception))
                    return@flow
                }

                else -> {}
            }

            // Start scanning
            when (val scanResult = startScanning()) {
                is WeighingScaleResult.Error -> {
                    emit(WeighingScaleResult.Error(scanResult.exception))
                    return@flow
                }

                else -> {}
            }

            // Listen for weight data
            getWeightData().collect { weightData ->

                if (weightData.weightStatus == 1) { // Stable weight
                    when (val bodyFatResult = calculateBodyFatData(sex, age, height, weightData)) {
                        is WeighingScaleResult.Success -> {

                            val (bodyFatData, moreFatData) = bodyFatResult.data

                            emit(
                                WeighingScaleResult.Success(
                                    WeightMeasurement(
                                        weightData = weightData,
                                        bodyFatData = bodyFatData,
                                        moreFatData = moreFatData
                                    )
                                )
                            )

                        }

                        is WeighingScaleResult.Error -> {
                            emit(WeighingScaleResult.Error(bodyFatResult.exception))
                        }

                        else -> {}
                    }
                } else {
                    // Real-time weight data
                    emit(
                        WeighingScaleResult.Success(
                            WeightMeasurement(
                                weightData = weightData,
                                bodyFatData = null,
                                moreFatData = null
                            )
                        )
                    )
                }
            }
        }
    }*/
}