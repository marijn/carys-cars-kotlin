package com.carshare.local

import com.carshare.domain.*
import com.carshare.repository.CustomerRepository
import com.carshare.repository.VehicleRepository
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/local")
@Profile("local")
class LocalDevController(
    private val vehicleRepository: VehicleRepository,
    private val customerRepository: CustomerRepository
) {

    /**
     * POST /api/local/seed
     * Seeds a realistic fleet of modern EVs compatible with the Smartcar API platform.
     * Safe to call multiple times — skips vehicles that already exist by smartcarId.
     *
     * Vehicle classes and their meaning:
     *   CITY_SMALL   — small city cars (Renault Zoe, Fiat 500e, VW ID.3)
     *   LONG_RANGE   — cars with extended range for longer trips (Tesla Model 3, Polestar 2, Hyundai IONIQ 5)
     *   BULKY        — moving bulky things: large 7-seater cars (Tesla Model X, Mercedes EQB) and moving vans (Mercedes eVito, VW ID. Buzz Cargo)
     *   FUN_PREMIUM  — premium and fun vehicles (BMW i4, Kia EV6 GT, Porsche Taycan)
     *
     * ACRISS codes (all end in AE = Automatic + Electric BEV):
     *   MDAE  Mini,         4-door,        Automatic, Electric — Renault Zoe, Fiat 500e
     *   CDAE  Compact,      4-door,        Automatic, Electric — VW ID.3, Tesla Model 3
     *   IFAE  Intermediate, SUV/Crossover, Automatic, Electric — Hyundai IONIQ 5, Kia EV6
     *   FFAE  Fullsize,     SUV,           Automatic, Electric — Tesla Model Y, Volvo EX40
     *   SFAE  Standard,     SUV,           Automatic, Electric — Tesla Model X, Mercedes EQB (7-seat)
     *   LWAE  Luxury,       Wagon/Estate,  Automatic, Electric — Polestar 2
     *   PFAE  Premium,      SUV,           Automatic, Electric — BMW iX, Porsche Taycan Sport Turismo
     *   CTAE  Compact,      Convertible,   Automatic, Electric — BMW i4
     *   KVAE  Van,          Passenger Van, Automatic, Electric — Mercedes eVito Tourer, VW ID. Buzz Cargo, Ford E-Transit Custom
     *
     * License plate formats by country:
     *   NL: LL-NNN-L   e.g. GH-481-K   (letters–digits–letter, current sidecodes, no vowels/C/Q)
     *   DE: [City]-[LL] NNNN  e.g. B-EV 4821 (Berlin), M-XR 2210 (Munich), D-CS 9931 (Düsseldorf)
     *   CH: [Canton] NNNNNN  e.g. ZH 445521
     *   DK: LL NN NNN  e.g. CK 74 291
     */
    @PostMapping("/seed")
    fun seed(): ResponseEntity<Map<String, Any>> {
        val existing = vehicleRepository.findAll().map { it.smartcarId }.toSet()

        val vehicles = listOf(
            // ── Amsterdam (NL) ────────────────────────────────────────────
            v("sc_ams_001", "GH-481-K",  "Renault",    "Zoe",             2022, "MDAE", VehicleClass.CITY_SMALL,  "Amsterdam", "NL", 52.3730, 4.8924),
            v("sc_ams_002", "KR-752-B",  "Fiat",       "500e",            2023, "MDAE", VehicleClass.CITY_SMALL,  "Amsterdam", "NL", 52.3602, 4.8857),
            v("sc_ams_003", "LT-219-Z",  "Volkswagen", "ID.3",            2023, "CDAE", VehicleClass.CITY_SMALL,  "Amsterdam", "NL", 52.3676, 4.9041),
            v("sc_ams_004", "NP-903-S",  "Tesla",      "Model 3",         2023, "CDAE", VehicleClass.LONG_RANGE,  "Amsterdam", "NL", 52.3788, 4.9000),
            v("sc_ams_005", "XB-114-H",  "Polestar",   "2",               2023, "LWAE", VehicleClass.LONG_RANGE,  "Amsterdam", "NL", 52.3553, 4.9195),
            v("sc_ams_006", "ZV-330-D",  "Tesla",      "Model X",         2023, "SFAE", VehicleClass.BULKY,   "Amsterdam", "NL", 52.3810, 4.8731),
            v("sc_ams_007", "DF-607-M",  "Mercedes",   "eVito Tourer",    2023, "KVAE", VehicleClass.BULKY, "Amsterdam", "NL", 52.3500, 4.9167),
            v("sc_ams_008", "RB-559-N",  "BMW",        "i4",              2023, "CTAE", VehicleClass.FUN_PREMIUM, "Amsterdam", "NL", 52.3650, 4.8800),

            // ── Rotterdam (NL) ────────────────────────────────────────────
            v("sc_rot_001", "BN-228-T",  "Volkswagen", "ID.3",            2022, "CDAE", VehicleClass.CITY_SMALL,  "Rotterdam", "NL", 51.9244, 4.4777),
            v("sc_rot_002", "PK-519-R",  "Hyundai",    "IONIQ 5",         2023, "IFAE", VehicleClass.LONG_RANGE,  "Rotterdam", "NL", 51.9150, 4.4820),
            v("sc_rot_003", "WG-843-F",  "Volkswagen", "ID. Buzz Cargo",  2023, "KVAE", VehicleClass.BULKY, "Rotterdam", "NL", 51.9300, 4.4700),
            v("sc_rot_004", "TF-317-K",  "Mercedes",   "EQB",             2023, "SFAE", VehicleClass.BULKY,   "Rotterdam", "NL", 51.9200, 4.4900),

            // ── Berlin (DE) ───────────────────────────────────────────────
            v("sc_ber_001", "B-EV 4821", "Volkswagen", "ID.3",            2023, "CDAE", VehicleClass.CITY_SMALL,  "Berlin",    "DE", 52.5200, 13.4050),
            v("sc_ber_002", "B-XR 2210", "Tesla",      "Model 3",         2022, "CDAE", VehicleClass.LONG_RANGE,  "Berlin",    "DE", 52.5100, 13.3900),
            v("sc_ber_003", "B-IY 9031", "BMW",        "i4",              2023, "CTAE", VehicleClass.FUN_PREMIUM, "Berlin",    "DE", 52.5300, 13.4200),
            v("sc_ber_004", "B-CS 7714", "Mercedes",   "eVito Tourer",    2023, "KVAE", VehicleClass.BULKY, "Berlin",    "DE", 52.5050, 13.4300),
            v("sc_ber_005", "B-MZ 6603", "Tesla",      "Model X",         2023, "SFAE", VehicleClass.BULKY,   "Berlin",    "DE", 52.4950, 13.3800),

            // ── Munich (DE) ───────────────────────────────────────────────
            v("sc_muc_001", "M-XR 1147", "Kia",        "EV6 GT",          2023, "IFAE", VehicleClass.FUN_PREMIUM, "Munich",    "DE", 48.1351, 11.5820),
            v("sc_muc_002", "M-IV 8830", "Volvo",      "EX40",            2023, "FFAE", VehicleClass.LONG_RANGE,  "Munich",    "DE", 48.1430, 11.5700),
            v("sc_muc_003", "M-EZ 3302", "Renault",    "Zoe",             2022, "MDAE", VehicleClass.CITY_SMALL,  "Munich",    "DE", 48.1270, 11.5900),
            v("sc_muc_004", "M-BV 4410", "Ford",       "E-Transit Custom",2023, "KVAE", VehicleClass.BULKY, "Munich",    "DE", 48.1500, 11.5600),

            // ── Düsseldorf (DE) ───────────────────────────────────────────
            v("sc_dus_001", "D-CS 9931", "Hyundai",    "IONIQ 5",         2023, "IFAE", VehicleClass.LONG_RANGE,  "Dusseldorf","DE", 51.2217, 6.7762),
            v("sc_dus_002", "D-EV 5512", "Mercedes",   "eVito Tourer",    2023, "KVAE", VehicleClass.BULKY, "Dusseldorf","DE", 51.2300, 6.7850),
            v("sc_dus_003", "D-GR 7721", "Fiat",       "500e",            2022, "MDAE", VehicleClass.CITY_SMALL,  "Dusseldorf","DE", 51.2150, 6.7700),

            // ── Zurich (CH) ───────────────────────────────────────────────
            v("sc_zur_001", "ZH 445521", "Tesla",      "Model Y",         2023, "FFAE", VehicleClass.LONG_RANGE,  "Zurich",    "CH", 47.3769, 8.5417),
            v("sc_zur_002", "ZH 112038", "Polestar",   "2",               2023, "LWAE", VehicleClass.FUN_PREMIUM, "Zurich",    "CH", 47.3850, 8.5300),
            v("sc_zur_003", "ZH 887643", "Fiat",       "500e",            2022, "MDAE", VehicleClass.CITY_SMALL,  "Zurich",    "CH", 47.3700, 8.5500),
            v("sc_zur_004", "ZH 334812", "Volkswagen", "ID. Buzz Cargo",  2023, "KVAE", VehicleClass.BULKY, "Zurich",    "CH", 47.3900, 8.5200),

            // ── Copenhagen (DK) ───────────────────────────────────────────
            v("sc_cph_001", "CK 74 291", "Volkswagen", "ID.3",            2023, "CDAE", VehicleClass.CITY_SMALL,  "Copenhagen","DK", 55.6761, 12.5683),
            v("sc_cph_002", "BF 31 847", "Kia",        "EV6 GT",          2023, "IFAE", VehicleClass.FUN_PREMIUM, "Copenhagen","DK", 55.6850, 12.5800),
            v("sc_cph_003", "MR 55 102", "Tesla",      "Model 3",         2022, "CDAE", VehicleClass.LONG_RANGE,  "Copenhagen","DK", 55.6680, 12.5550),
            v("sc_cph_004", "TL 28 934", "Mercedes",   "eVito Tourer",    2023, "KVAE", VehicleClass.BULKY, "Copenhagen","DK", 55.6900, 12.5400),
        )

        val created = vehicles
            .filter { it.smartcarId !in existing }
            .map { vehicleRepository.save(it) }

        return ResponseEntity.ok(mapOf(
            "created" to created.size,
            "total"   to vehicleRepository.count()
        ))
    }

    /**
     * POST /api/local/kyc/approve?customerId=...
     * Instantly approves KYC for any customer.
     */
    @PostMapping("/kyc/approve")
    fun approveKyc(@RequestParam customerId: UUID): ResponseEntity<String> {
        val customer = customerRepository.findById(customerId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        customer.kycStatus = KycStatus.APPROVED
        customerRepository.save(customer)
        return ResponseEntity.ok("KYC approved for ${customer.email}")
    }

    /**
     * GET /api/local/me
     * Returns the demo customer so you can grab their ID for other calls.
     */
    @GetMapping("/me")
    fun me(): ResponseEntity<Any> {
        val customer = customerRepository.findByOauthSubject(LocalUserFilter.LOCAL_SUBJECT)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(mapOf(
            "id"                    to customer.id,
            "email"                 to customer.email,
            "kycStatus"             to customer.kycStatus,
            "stripePaymentMethodId" to customer.stripePaymentMethodId
        ))
    }

    private fun v(
        smartcarId: String, plate: String,
        make: String, model: String, year: Int, acriss: String,
        cls: VehicleClass, city: String, country: String,
        lat: Double, lon: Double
    ) = Vehicle(
        smartcarId   = smartcarId,
        licensePlate = plate,
        make         = make,
        model        = model,
        year         = year,
        acrissCode   = acriss,
        vehicleClass = cls,
        status       = VehicleStatus.AVAILABLE,
        batteryLevel = (60..95).random(),
        odometerKm   = (254..49_999).random().toDouble(),
        latitude     = lat,
        longitude    = lon,
        city         = city,
        country      = country
    )
}
