package app.naerio.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 상세 조작과 경로 변경 허용 여부. UNKNOWN은 허용 근거로 쓰지 않는다. */
enum class Interaction { ALLOWED, RESTRICTED, UNKNOWN }

interface InteractionPolicyProvider {
    val state: StateFlow<Interaction>
    val source: String
    fun start()
    fun stop()
}

/**
 * AAOS CarUxRestrictionsManager 기반. 속도 0을 주차로 간주하지 않고 플랫폼의 주행 제한 판정을 따른다.
 * Distraction Optimization이 필요하다는 판정(isRequiresDistractionOptimization)이면 제한 상태로 본다.
 */
class PleosInteractionPolicyProvider(private val context: Context) : InteractionPolicyProvider {
    private val _state = MutableStateFlow(Interaction.UNKNOWN)
    override val state = _state.asStateFlow()
    override val source = "CarUxRestrictionsManager"
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { update(it) }

    override fun start() {
        try {
            val c = Car.createCar(context) ?: return
            car = c
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            manager = m
            if (m == null) { _state.value = Interaction.UNKNOWN; return }
            m.registerListener(listener)
            update(m.currentCarUxRestrictions)
        } catch (e: Throwable) {
            Log.w("NaerilgotUx", "UX restrictions unavailable", e)
            _state.value = Interaction.UNKNOWN
        }
    }

    private fun update(r: CarUxRestrictions?) {
        _state.value = when {
            r == null -> Interaction.UNKNOWN
            r.isRequiresDistractionOptimization -> Interaction.RESTRICTED
            else -> Interaction.ALLOWED
        }
        Log.d("NaerilgotUx", "ux restrictions -> ${_state.value} (${r?.activeRestrictions})")
    }

    override fun stop() {
        try { manager?.unregisterListener() } catch (_: Throwable) {}
        try { car?.disconnect() } catch (_: Throwable) {}
        manager = null; car = null
    }
}

/** 시연 모드에서만 쓰는 모의 입력. 실제 차량 정책을 우회하지 않는다. */
class SimulatedInteractionPolicyProvider : InteractionPolicyProvider {
    private val _state = MutableStateFlow(Interaction.ALLOWED)
    override val state = _state.asStateFlow()
    override val source = "시연용 모의 입력"
    fun set(v: Interaction) { _state.value = v }
    override fun start() {}
    override fun stop() {}
}
