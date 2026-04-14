package com.carshare.controller

import com.carshare.domain.Customer
import com.carshare.external.IStripeClient
import com.carshare.external.IVeriffClient
import com.carshare.repository.CustomerDailyReservationUsageRepository
import com.carshare.repository.CustomerRepository
import com.carshare.repository.ReservationRepository
import com.carshare.repository.VehicleRepository
import com.carshare.service.CustomerService
import com.carshare.service.ReservationService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*
import java.util.function.Function

class ReservationControllerTest {
    @Test
    fun `POST reservations`() {
        // Arrange
        val fakeCustomerRepository: CustomerRepository = object : CustomerRepository {
            private var customer: Customer? = null

            override fun findByOauthSubject(subject: String): Customer? {
                return null
            }

            override fun findByEmail(email: String): Customer? {
                TODO("Not yet implemented")
            }

            override fun flush() {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> saveAndFlush(entity: S & Any): S & Any {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> saveAllAndFlush(entities: Iterable<S?>): List<S?> {
                TODO("Not yet implemented")
            }

            override fun deleteAllInBatch(entities: Iterable<Customer?>) {
                TODO("Not yet implemented")
            }

            override fun deleteAllByIdInBatch(ids: Iterable<UUID?>) {
                TODO("Not yet implemented")
            }

            override fun deleteAllInBatch() {
                TODO("Not yet implemented")
            }

            override fun getOne(id: UUID): Customer {
                TODO("Not yet implemented")
            }

            override fun getById(id: UUID): Customer {
                TODO("Not yet implemented")
            }

            override fun getReferenceById(id: UUID): Customer {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> findAll(example: Example<S?>): List<S?> {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> findAll(example: Example<S?>, sort: Sort): List<S?> {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> saveAll(entities: Iterable<S?>): List<S?> {
                TODO("Not yet implemented")
            }

            override fun findAll(): List<Customer?> {
                TODO("Not yet implemented")
            }

            override fun findAllById(ids: Iterable<UUID?>): List<Customer?> {
                TODO("Not yet implemented")
            }

            override fun findAll(sort: Sort): List<Customer?> {
                TODO("Not yet implemented")
            }

            override fun findAll(pageable: Pageable): Page<Customer?> {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> findOne(example: Example<S?>): Optional<S?> {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> findAll(example: Example<S?>, pageable: Pageable): Page<S?> {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> count(example: Example<S?>): Long {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> exists(example: Example<S?>): Boolean {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?, R : Any?> findBy(
                example: Example<S?>,
                queryFunction: Function<FluentQuery.FetchableFluentQuery<S?>?, R?>
            ): R & Any {
                TODO("Not yet implemented")
            }

            override fun <S : Customer?> save(entity: S & Any): S & Any {
                this.customer = entity
                return entity
            }

            override fun findById(id: UUID): Optional<Customer> {
                return Optional.ofNullable(customer)
            }

            override fun existsById(id: UUID): Boolean {
                TODO("Not yet implemented")
            }

            override fun count(): Long {
                TODO("Not yet implemented")
            }

            override fun deleteById(id: UUID) {
                TODO("Not yet implemented")
            }

            override fun delete(entity: Customer) {
                TODO("Not yet implemented")
            }

            override fun deleteAllById(ids: Iterable<UUID?>) {
                TODO("Not yet implemented")
            }

            override fun deleteAll(entities: Iterable<Customer?>) {
                TODO("Not yet implemented")
            }

            override fun deleteAll() {
                TODO("Not yet implemented")
            }
        }
        val customerService = CustomerService(
            fakeCustomerRepository,
            mock<IStripeClient>(),
            mock<IVeriffClient>()
        )
        val controller = ReservationController(
            ReservationService(
                mock<ReservationRepository>(),
                mock<VehicleRepository>(),
                customerService,
                mock<CustomerDailyReservationUsageRepository>(),
                20,
                20,
                20
            ),
            customerService
        )

        assertThatThrownBy({
            controller.createReservation(
                Jwt.withTokenValue("Bearer")
                    .header("Some header", {})
                    .claim("Some claim", {})
                    .subject("customer:11111111-1111-1111-1111-111111111111")
                    .build(),
                UUID.fromString("12341234-1234-1234-1234-123412341234")
            );
        }).hasMessage("Customer not ready to rent (KYC or payment missing)")
    }
}