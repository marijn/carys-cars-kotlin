# CarShare MVP Backend

Kotlin + Spring Boot 3 REST API for a free-floating electric car-sharing service.

---

## Stack

| Layer | Tech |
|---|---|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 + PostGIS |
| Migrations | Flyway |
| Auth | OAuth2 JWT (any OIDC provider) |
| Payments | Stripe |
| Fleet | Smartcar API |
| KYC | Veriff |

---

## Quick Start

```bash
# 1. Copy and fill env vars
cp .env.example .env

# 2. Start DB + app
docker compose up

# App runs on http://localhost:8080
```

---

## Environment Variables

```
OAUTH_ISSUER_URI=https://your-auth-provider.com
STRIPE_API_KEY=sk_...
STRIPE_WEBHOOK_SECRET=whsec_...
SMARTCAR_CLIENT_ID=...
SMARTCAR_CLIENT_SECRET=...
VERIFF_API_KEY=...
DATABASE_URL=jdbc:postgresql://localhost:5432/carshare
DATABASE_USER=carshare
DATABASE_PASSWORD=carshare
```

---

## API Reference

All authenticated endpoints require `Authorization: Bearer <jwt>`.

---

### Customers

#### Register / Get current user
```
POST /api/customers/register
GET  /api/customers/me
```

**POST body:**
```json
{ "email": "jane@example.com", "fullName": "Jane Doe" }
```

#### Start KYC (driver's license verification via Veriff)
```
POST /api/customers/me/kyc/start
```
Returns a Veriff redirect URL. Customer completes verification in their browser.

**Response:**
```json
{ "veriffUrl": "https://magic.veriff.me/v/..." }
```

#### Add payment method (Stripe)
```
POST /api/customers/me/payment-method
```
```json
{ "paymentMethodId": "pm_..." }
```
Create the PaymentMethod on the frontend using Stripe.js, then send the ID here.

---

### Vehicles

#### Browse available vehicles
```
GET /api/vehicles?city=Amsterdam&vehicleClass=CITY_SMALL
```
Both params are optional. Public endpoint — no auth required.

**Vehicle classes:**
| Value | Description |
|---|---|
| `CITY_SMALL` | Small city cars |
| `BULKY_BIG` | Big cars for bulky items |
| `BULKY_TRUCK` | Trucks |
| `LONG_RANGE` | Cars with extended range |
| `FUN_PREMIUM` | Premium / fun cars |

#### Get single vehicle
```
GET /api/vehicles/{id}
```

#### View damage reports for a vehicle
```
GET /api/vehicles/{id}/damage-reports
```
Call this before starting a reservation so the customer can compare.

#### Report new damage (required before renting)
```
POST /api/vehicles/{id}/damage-reports
```
```json
{
  "position": "FRONT",
  "photoUrl": "https://storage.example.com/photo.jpg",
  "licensePlatePhotoUrl": "https://storage.example.com/plate.jpg",
  "notes": "Small scratch on front bumper"
}
```
`position` values: `FRONT`, `BACK`, `LEFT`, `RIGHT`

Photos should be uploaded to your own object storage (S3, GCS, etc.) first, then the URL passed here.

---

### Reservations

The flow is: **browse → report any new damage → reserve → start rental → drive → end rental**

#### Create reservation
```
POST /api/reservations?vehicleId={uuid}
```
- Vehicle moves to `RESERVED` status immediately
- Reservation expires after **20 minutes** by default
- Customer gets **20 free reservation minutes per day**; beyond that: **€0.20/minute**

#### Extend reservation
```
POST /api/reservations/{id}/extend
```
```json
{ "additionalMinutes": 10 }
```

#### Cancel reservation
```
DELETE /api/reservations/{id}
```
Vehicle returns to `AVAILABLE`. Overage invoice issued if applicable.

---

### Rentals

#### Start rental (converts reservation → active rental)
```
POST /api/rentals/start?reservationId={uuid}
```
- Unlocks the vehicle via Smartcar
- Records start location
- Issues reservation overage invoice if applicable

#### Get active rental
```
GET /api/rentals/active
```

#### End rental
```
POST /api/rentals/{id}/end
```
- Vehicle must be within **30km of an operating city** (any of the 7 cities)
- Locks the vehicle via Smartcar
- Calculates cost and issues invoice automatically

**Pricing:**
- **€0.39/minute** (minimum 1 minute)
- First **200 km** included
- Beyond 200 km: **€0.30/km**

**Invoice is immediately charged** to the card on file via Stripe.

---

### KYC Webhook

```
POST /api/kyc/callback
```
Called by Veriff. Public endpoint. Updates customer KYC status to `APPROVED` or `REJECTED`.

---

## Operating Cities

| City | Country |
|---|---|
| Amsterdam | NL |
| Rotterdam | NL |
| Berlin | DE |
| Munich | DE |
| Düsseldorf | DE |
| Zurich | CH |
| Copenhagen | DK |

You can drive between cities freely. You **cannot end a rental outside these city areas**. Cross-border driving is allowed; ending in a different country than you started is allowed.

---

## Business Rules Summary

| Rule | Value |
|---|---|
| Rental rate | €0.39 / minute |
| Minimum rental | 1 minute |
| Distance included | 200 km / trip |
| Distance overage | €0.30 / km |
| Free reservation time | 20 min / day |
| Reservation overage | €0.20 / min |
| Default reservation expiry | 20 min |
| Battery charge threshold | 15% |
| Must end in operating city | Yes (30km radius) |
| Cannot end in different country | No (cross-country ok) |

---

## Customer States (happy path)

```
Sign up (OAuth)
    → Initiate KYC → Veriff callback → KYC approved
    → Add payment method
    → Browse vehicles
    → (Optional) Report new damage
    → Create reservation
    → Start rental        ← vehicle unlocked
    → Drive
    → End rental          ← vehicle locked, invoice charged
```

---

## Notes for Production

- **Photo uploads**: implement S3/GCS presigned URL endpoint — the API currently accepts photo URLs, assuming upload happens client-side first
- **Smartcar tokens**: each vehicle needs its own OAuth token; implement token storage + refresh
- **City geo boundaries**: the MVP uses a 30km radius haversine check; replace with PostGIS polygon boundaries for accuracy
- **Failed payments**: currently stored as `FAILED` invoice status — add dunning/retry logic
- **Smartcar signals (webhooks)**: hook up Smartcar's event webhooks to update battery/location in real time instead of polling

---

## Local Development (no config needed)

Run with the `local` Spring profile to get a fully working app with no external dependencies:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Or in IntelliJ: set `Active profiles` to `local` in the run configuration.

What the local profile does:
- Uses **H2 in-memory database** — no Postgres needed
- **All endpoints are open** — no JWT token required
- **Demo user is auto-created** — KYC approved, payment method set
- Smartcar, Stripe and Veriff are **stubbed** — calls are logged, nothing is charged

### Demo walkthrough

**1. Seed the fleet**
```
POST http://localhost:8080/api/local/seed
```

**2. Get your demo customer ID**
```
GET http://localhost:8080/api/local/me
```

**3. Browse available vehicles**
```
GET http://localhost:8080/api/vehicles
GET http://localhost:8080/api/vehicles?city=Amsterdam
GET http://localhost:8080/api/vehicles?vehicleClass=CITY_SMALL
```

**4. View damage reports for a vehicle**
```
GET http://localhost:8080/api/vehicles/{vehicleId}/damage-reports
```

**5. Reserve a vehicle**
```
POST http://localhost:8080/api/reservations?vehicleId={vehicleId}
```

**6. Start the rental**
```
POST http://localhost:8080/api/rentals/start?reservationId={reservationId}
```

**7. End the rental (invoice is generated and logged)**
```
POST http://localhost:8080/api/rentals/{rentalId}/end
```

All Smartcar calls return Amsterdam centre coordinates, so the rental always ends in a valid city.
Stripe charges are logged to the console but never actually executed.
