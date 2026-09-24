package org.geonav.app.car

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.EnergyProfile
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

data class VehicleState(
    val speedKmh: Float? = null,
    val fuelPercent: Float? = null,
    val batteryPercent: Float? = null,
    val isAvailable: Boolean = false,
    val vehicleModel: String? = null
)

class VehicleDataManager(private val carContext: CarContext) {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private var isListening = false

    fun startListening(executor: Executor) {
        if (isListening) return
        isListening = true

        try {
            val hardwareManager = carContext.getCarService(CarHardwareManager::class.java)
            val carInfo = hardwareManager.carInfo

            // Model
            try {
                carInfo.fetchModel(executor) { model: Model ->
                    val name = "${model.manufacturer.value ?: ""} ${model.name.value ?: ""}".trim()
                    _vehicleState.value = _vehicleState.value.copy(
                        vehicleModel = if (name.isNotBlank()) name else null,
                        isAvailable = true
                    )
                }
            } catch (e: Exception) {
                Log.d("VehicleDataManager", "Vehicle model not exposed: ${e.message}")
            }

            // Real vehicle speed
            try {
                carInfo.addSpeedListener(executor) { speed: Speed ->
                    val rawSpeedMps = speed.rawSpeedMetersPerSecond.value
                    val speedKmh = if (rawSpeedMps != null && rawSpeedMps >= 0f) rawSpeedMps * 3.6f else null
                    _vehicleState.value = _vehicleState.value.copy(
                        speedKmh = speedKmh,
                        isAvailable = true
                    )
                }
            } catch (e: Exception) {
                Log.d("VehicleDataManager", "Speed listener not supported by vehicle: ${e.message}")
            }

            // Energy / Fuel level
            try {
                carInfo.addEnergyLevelListener(executor) { energy: EnergyLevel ->
                    val fuel = energy.fuelPercent.value
                    val battery = energy.batteryPercent.value
                    _vehicleState.value = _vehicleState.value.copy(
                        fuelPercent = fuel,
                        batteryPercent = battery,
                        isAvailable = true
                    )
                }
            } catch (e: Exception) {
                Log.d("VehicleDataManager", "Energy level not supported: ${e.message}")
            }
        } catch (e: Exception) {
            Log.d("VehicleDataManager", "Car hardware manager not available: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        // Listeners cleaned up with session
    }
}
