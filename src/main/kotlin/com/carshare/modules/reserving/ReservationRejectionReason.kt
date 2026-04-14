package com.carshare.modules.reserving

sealed class ReservationRejectionReason {
    data class AlreadyReserved(
        private val currentlyReservedBy: String
    ): ReservationRejectionReason();
}