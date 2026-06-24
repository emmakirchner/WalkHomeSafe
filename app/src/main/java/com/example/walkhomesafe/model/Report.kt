package com.example.walkhomesafe.model

enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class DangerCategory(val displayName: String) {
    BELEUCHTUNG("Beleuchtung"),
    KRIMINALITAET("Kriminalität"),
    WEGSCHADEN("Wegschaden"),
    BELASTIGUNG("Belästigung"),
    NATURGEFAHR("Naturgefahr"),
    SONSTIGES("Sonstiges"),
    SICHERHEIT("Sicherheit")
}

data class Report(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val category: DangerCategory = DangerCategory.SONSTIGES,
    val description: String? = null,
    val severity: Severity,
    val timestamp: Long = 0L,
    val isPositive: Boolean = false
)
