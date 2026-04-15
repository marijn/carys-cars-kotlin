package com.carshare.modules

import com.carshare.infrastructure.messaging.Event
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
): Event, AnyUnlockVehicleAutomationEvent

data class RentalEnded (
    public val rentalId: String,
    public val customerId: String,
    public val rentalStarted: LocalDateTime
): TrialAutomationEvent, TrialProjectEvents
