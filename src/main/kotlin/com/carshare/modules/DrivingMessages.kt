package com.carshare.modules

import com.carshare.infrastructure.messaging.Command

data class UnlockVehicle(
    /**
     * @example "NL:GGS-10-N"
     */
    val vehicle: String
): Command