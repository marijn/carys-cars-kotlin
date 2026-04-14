package com.carshare.modules

sealed class LicensePlate {
    data class DutchLicensePlate(val plateNo: String): LicensePlate();
    data class GermanLicensePlate(val plateNo: String): LicensePlate();
    data class SwissLicensePlate(val plateNo: String): LicensePlate();
    data class DanishLicensePlate(val plateNo: String): LicensePlate();
}