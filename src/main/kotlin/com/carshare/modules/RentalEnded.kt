package com.carshare.modules

import java.time.LocalDateTime

data class RentalEnded (
    public val rentalId: String,
    public val customerId: String,
    public val rentalStarted: LocalDateTime
): TrialAutomationEvent, TrialProjectEvents