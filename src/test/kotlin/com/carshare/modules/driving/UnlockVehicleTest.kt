package com.carshare.modules.driving

import com.carshare.infrastructure.automation.testing.AutomationScenario
import com.carshare.infrastructure.messaging.Command
import com.carshare.modules.AnyUnlockVehicleAutomationEvent
import com.carshare.modules.UnlockVehicle
import com.carshare.modules.RentalStarted
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UnlockVehicleTest {
    @Test()
    fun `once the rental has been started`() {
        AutomationScenario<AnyUnlockVehicleAutomationEvent, Command>()
            .whenTriggeredBecauseOf(
                RentalStarted(
                    "agreement:11111111-1111-1111-1111-111111111111",
                    "customer:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                    "3498,8",
                    "52,37342° N, 4,88641° E",
                    LocalDateTime.parse("2024-09-12T10:22"),
                    "NL:JLP-51-J"
                )
            )
            .thenExpect(
                UnlockVehicle(
                    "NL:JLP-51-J"
                )
            )
            .assertOnProcessManager(UnlockVehicleProcessManager())
    }
}
