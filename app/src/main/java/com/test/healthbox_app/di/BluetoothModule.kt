package com.test.healthbox_app.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.test.healthbox_app.data.ble.BleConnectionManager
import com.test.healthbox_app.data.ble.BleScanner
import com.test.healthbox_app.data.ble.QppManager
import com.test.healthbox_app.data.data_source.BiosenseDataSource
import com.test.healthbox_app.data.data_source.SharedPreferencesManager
import com.test.healthbox_app.data.repository.BleRepositoryImpl
import com.test.healthbox_app.data.repository.BluetoothRepositoryImpl
import com.test.healthbox_app.data.repository.MeasurementRepositoryImpl
import com.test.healthbox_app.data.repository.PermissionRepositoryImpl
import com.test.healthbox_app.data.repository.SharedPreferenceRepositoryImpl
import com.test.healthbox_app.data.repository.WeighingScaleRepositoryImpl
import com.test.healthbox_app.di.factory.PermissionHandlerFactory
import com.test.healthbox_app.domain.repository.BleRepository
import com.test.healthbox_app.domain.repository.BluetoothRepository
import com.test.healthbox_app.domain.repository.MeasurementRepository
import com.test.healthbox_app.domain.repository.PermissionRepository
import com.test.healthbox_app.domain.repository.SharePreferenceRepository
import com.test.healthbox_app.domain.repository.WeighingScaleRepository
import com.test.healthbox_app.domain.use_cases.BleUseCases
import com.test.healthbox_app.domain.use_cases.BluetoothUseCases
import com.test.healthbox_app.domain.use_cases.CheckAndRequestPermissionsUseCase
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import com.test.healthbox_app.domain.use_cases.WeighingScaleUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter? {
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context, bluetoothAdapter: BluetoothAdapter?
    ): BluetoothRepository {
        return BluetoothRepositoryImpl(context = context, bluetoothAdapter = bluetoothAdapter)
    }

    @Provides
    @Singleton
    fun provideBluetoothUseCase(
        bluetoothRepository: BluetoothRepository
    ): BluetoothUseCases {
        return BluetoothUseCases(repository = bluetoothRepository)
    }

    @Provides
    @Singleton
    fun provideBleConnectionManager(
        @ApplicationContext context: Context,
        qppManager: QppManager
    ): BleConnectionManager {
        return BleConnectionManager(
            context = context,
            qppManager = qppManager
        )
    }

    @Provides
    @Singleton
    fun provideBleRepository(
        @ApplicationContext context: Context,
        bleScanner: BleScanner,
        bleConnectionManager: BleConnectionManager,
        biosenseDataSource: BiosenseDataSource
    ): BleRepository {
        return BleRepositoryImpl(
            context = context, bleScanner = bleScanner, bleConnectionManager = bleConnectionManager, biosenseDataSource = biosenseDataSource
        )
    }

    @Provides
    @Singleton
    fun provideBleUseCases(
        bleRepository: BleRepository
    ): BleUseCases {
        return BleUseCases(
//            dispatcher = dispatcher,
            bleRepository = bleRepository
        )
    }

    @Provides
    @Singleton
    fun provideBleScanner(@ApplicationContext context: Context): BleScanner {
        return BleScanner(context = context)
    }

    @Provides
    @Singleton
    fun provideCheckAndRequestPermissionsUseCase(
        permissionRepository: PermissionRepository
    ): CheckAndRequestPermissionsUseCase {
        return CheckAndRequestPermissionsUseCase(permissionRepository = permissionRepository)
    }

    @Provides
    @Singleton
    fun providePermissionRepository(
        @ApplicationContext context: Context,
    ): PermissionRepository {
        return PermissionRepositoryImpl(context = context)
    }

    // Note: PermissionHandler needs a Fragment and ActivityResultLauncher
    // which aren't available at injection time, so we'll create a factory instead

    @Module
    @InstallIn(SingletonComponent::class)
    object PermissionHandlerFactoryModule {
        @Provides
        @Singleton
        fun providePermissionHandlerFactory(): PermissionHandlerFactory {
            return PermissionHandlerFactory()
        }
    }

    @Provides
    @Singleton
    fun provideSharedPreferencesManager(@ApplicationContext context: Context): SharedPreferencesManager {
        return SharedPreferencesManager(context = context)
    }

    @Provides
    @Singleton
    fun provideBleDeviceRepository(sharedPreferencesManager: SharedPreferencesManager): SharePreferenceRepository {
        return SharedPreferenceRepositoryImpl(sharedPreferencesManager = sharedPreferencesManager)
    }

    @Provides
    @Singleton
    fun provideSaveBleDeviceUseCase(repository: SharePreferenceRepository): SharedPreferenceUseCases {
        return SharedPreferenceUseCases(sharePreferenceRepository = repository)
    }

    @Provides
    @Singleton
    fun provideMeasurementRepository(): MeasurementRepository {
        return MeasurementRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideWeighingScaleRepository(@ApplicationContext context: Context): WeighingScaleRepository {
        return WeighingScaleRepositoryImpl(context = context)
    }

    @Provides
    @Singleton
    fun provideWeighingScaleUseCases(repository: WeighingScaleRepository): WeighingScaleUseCases {
        return WeighingScaleUseCases(repository = repository)
    }
}