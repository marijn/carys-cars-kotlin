package com.carshare.modules

import com.carshare.infrastructure.automation.ProcessManager
import com.carshare.infrastructure.automation.testing.AutomationScenario
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TrialAutomationTest {
    @Test
    fun `It` () {
        val scenario = AutomationScenario<TrialAutomationEvent, TrialAutomationCommand>()
            .whenTriggeredBecauseOf(
                RentalEnded(
                    "rental:1235"
                )
            )
            .thenExpect(
                AnyReservationCommand.PleaseReserveVehicle(
                    LicensePlate.DutchLicensePlate("GGR-12-X"),
                    "customer:11111111-1111-1111-1111-111111111111",
                    LocalDateTime.now()
                )
            )
            .assertOnProcessManager(TrialAutomation())
    }
}

class TrialAutomation: ProcessManager<TrialAutomationEvent, TrialAutomationCommand> {
    override fun processEvent(trigger: TrialAutomationEvent): List<TrialAutomationCommand> {
        TODO("Not yet implemented")
    }
}
