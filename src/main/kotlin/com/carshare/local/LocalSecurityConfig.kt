package com.carshare.local

import com.carshare.domain.KycStatus
import com.carshare.repository.CustomerRepository
import com.carshare.domain.Customer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

@Configuration
@Profile("local")
class LocalSecurityConfig {

    @Bean
    fun localSecurityFilterChain(
        http: HttpSecurity,
        localUserFilter: LocalUserFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .addFilterBefore(localUserFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

/**
 * Injects a fixed fake JWT principal for every request so that
 * @AuthenticationPrincipal Jwt works in controllers without a real token.
 * Also auto-creates the demo customer in the database on first request.
 */
@Component
@Profile("local")
class LocalUserFilter(
    private val customerRepository: CustomerRepository
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(LocalUserFilter::class.java)

    companion object {
        const val LOCAL_SUBJECT = "local|demo-user"
        const val LOCAL_EMAIL   = "demo@carshare.local"
        const val LOCAL_NAME    = "Demo User"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        ensureDemoCustomerExists()

        val jwt = Jwt.withTokenValue("local-token")
            .header("alg", "none")
            .subject(LOCAL_SUBJECT)
            .claim("email", LOCAL_EMAIL)
            .claim("name", LOCAL_NAME)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val auth = UsernamePasswordAuthenticationToken(
            jwt, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth
        chain.doFilter(request, response)
    }

    private fun ensureDemoCustomerExists() {
        if (customerRepository.findByOauthSubject(LOCAL_SUBJECT) == null) {
            customerRepository.save(Customer(
                oauthSubject = LOCAL_SUBJECT,
                email = LOCAL_EMAIL,
                fullName = LOCAL_NAME,
                stripeCustomerId = "cus_local_demo",
                stripePaymentMethodId = "pm_local_demo",
                kycStatus = KycStatus.APPROVED
            ))
            log.info("[LOCAL] Created demo customer: $LOCAL_EMAIL (KYC approved, payment method set)")
        }
    }
}
