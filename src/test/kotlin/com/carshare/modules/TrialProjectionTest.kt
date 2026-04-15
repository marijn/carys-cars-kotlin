package com.carshare.modules

import com.carshare.infrastructure.projection.Projection
import com.carshare.infrastructure.projection.testing.ProjectionScenario
import org.junit.jupiter.api.Test

class TrialProjectionTest {
    @Test
    fun `No trips were made` () {
        val scenario = ProjectionScenario<AnyTrialProjectEvents, TrialProjectionQuestions, TrialProjectionAnswers>()
            .whenAskedFor(
                WhatTripsHaveBeenTakenDuringTheReportingPeriod(
                    "customer:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                    "2024M11"
                )
            )
            .thenExpect(
                TripsInReportingPeriod(
                    "customer:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                    "2024M11",
                    listOf()
                )
            )
            .assertOnProjection(TrialProjection())
    }
}

class TrialProjection: Projection<AnyTrialProjectEvents, TrialProjectionQuestions, TrialProjectionAnswers> {
    override fun acknowledge(event: AnyTrialProjectEvents) {
        when(event) {
            is RentalEnded -> Unit
        }
    }

    override fun ask(question: TrialProjectionQuestions): TrialProjectionAnswers {
        return when(question) {
            is WhatTripsHaveBeenTakenDuringTheReportingPeriod ->
                TripsInReportingPeriod(
                    question.customer,
                    question.reportingPeriod,
                    listOf()
                )
        }
    }
}

