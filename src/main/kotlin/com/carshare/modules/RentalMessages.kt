package com.carshare.modules

import java.time.LocalDateTime

data class RentalEnded (
    val rentalId: String,
    val customerId: String,
    val rentalStarted: LocalDateTime
): AnyTrialAutomationEvent, AnyTrialProjectEvents