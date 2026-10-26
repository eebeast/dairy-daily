package com.milksummer.tracker.domain.model

import java.math.BigDecimal

data class SlipComparison(val difference: BigDecimal) {
    val matches: Boolean get() = difference <= BigDecimal("0.50")
}

fun compareSlip(quantity: Double, rate: Double, slipTotal: Double): SlipComparison? {
    if (!quantity.isFinite() || !rate.isFinite() || !slipTotal.isFinite()) return null
    return SlipComparison((quantity.toBigDecimal() * rate.toBigDecimal() - slipTotal.toBigDecimal()).abs())
}
