package com.carshare.modules.driving

import com.carshare.infrastructure.automation.ProcessManager
import com.carshare.infrastructure.automation.testing.AutomationScenario
import com.carshare.infrastructure.messaging.Command
import com.carshare.infrastructure.messaging.Event
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

data class RentalStarted(
    /**
     * @example "agreement:d77d8ff5-ab2f-401d-83dc-a6631ec87b38"
     */
    val agreementId: String,

    /**
     * @example "customer:92d68941-8783-471f-b3b8-7dbf7b803157"
     */
    val customerId: String,

    /**
     * @example "12598,2"
     */
    val odometerStart: String,

    /**
     * @example "52,35633° N, 4,83712° E"
     */
    val locationStart: String,

    /**
     * @example "2024-09-12 10:22 Europe/Amsterdam"
     */
    val rentalStarted: LocalDateTime,

    /**
     * @example "NL:GGS-10-N"
     */
    val vehicle: String
): Event()

data class UnlockVehicle(
    /**
     * @example "NL:GGS-10-N"
     */
    val vehicle: String
): Command()

class UnlockVehicleAutomation: ProcessManager {
    override fun processEvent(trigger: Event): List<Command> {
        TODO("Not yet implemented")
    }
}

class UnlockVehicleTest {
    @Test()
    fun `once the rental has been started`() {
        AutomationScenario()
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
            .assertOnProcessManager(UnlockVehicleAutomation())
    }
}
