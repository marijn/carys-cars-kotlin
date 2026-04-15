package com.carshare.modules

import com.carshare.infrastructure.messaging.Answer
import com.carshare.infrastructure.messaging.Event
import com.carshare.infrastructure.messaging.Question

sealed interface AnyTrialProjectEvents: Event {

}

sealed interface TrialProjectionQuestions: Question {

}

sealed interface TrialProjectionAnswers: Answer {

}

data class WhatTripsHaveBeenTakenDuringTheReportingPeriod (
    val customer: CustomerId,
    val reportingPeriod: String
): TrialProjectionQuestions

data class HistoricTrip (
    val rentalId: String,
)
data class TripsInReportingPeriod (
    val customer: CustomerId,
    val reportingPeriod: String,
    val historicTrips: List<HistoricTrip>
): TrialProjectionAnswers
