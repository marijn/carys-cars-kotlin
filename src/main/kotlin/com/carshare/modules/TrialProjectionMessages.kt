package com.carshare.modules

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question

sealed interface AnyTrialProjectEvents: Event {

}

sealed interface AnyTrialProjectionQuestions: Question {

}

sealed interface AnyTrialProjectionAnswers: Answer {

}

data class WhatTripsHaveBeenTakenDuringTheReportingPeriod (
    val customer: CustomerId,
    val reportingPeriod: String
): AnyTrialProjectionQuestions

data class HistoricTrip (
    val rentalId: String,
)
data class TripsInReportingPeriod (
    val customer: CustomerId,
    val reportingPeriod: String,
    val historicTrips: List<HistoricTrip>
): AnyTrialProjectionAnswers
