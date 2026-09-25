package com.example.locationalarm

import com.example.locationalarm.data.model.AlarmState
import com.example.locationalarm.data.model.Destination
import com.example.locationalarm.data.repository.AlarmRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmRepositoryTest {

    private lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        repository = AlarmRepository()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(AlarmState.Idle, repository.alarmState.value)
    }

    @Test
    fun setServiceActive_updatesAlarmStateToActive() = runTest {
        val testDest = Destination(40.7, -73.9, "Test Stop", 600f)
        repository.updateDestination(testDest)
        repository.setServiceActive(testDest)

        val state = repository.alarmState.value
        assertTrue(state is AlarmState.Active)
        assertEquals(testDest, (state as AlarmState.Active).destination)
    }

    @Test
    fun updateCurrentDistance_updatesActiveState() = runTest {
        val testDest = Destination.DEFAULT
        repository.setServiceActive(testDest)
        repository.updateCurrentDistance(350f)

        val state = repository.alarmState.value
        assertTrue(state is AlarmState.Active)
        assertEquals(350f, (state as AlarmState.Active).currentDistanceMeters)
    }

    @Test
    fun triggerAlarm_updatesStateToTriggered() = runTest {
        val testDest = Destination.DEFAULT
        repository.triggerAlarm(testDest, 150f)

        val state = repository.alarmState.value
        assertTrue(state is AlarmState.Triggered)
        assertEquals(150f, (state as AlarmState.Triggered).triggerDistanceMeters)
    }

    @Test
    fun stopAlarm_resetsStateToIdle() = runTest {
        repository.triggerAlarm(Destination.DEFAULT, 100f)
        repository.stopAlarm()

        assertEquals(AlarmState.Idle, repository.alarmState.value)
    }
}
