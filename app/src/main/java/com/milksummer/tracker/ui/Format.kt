package com.milksummer.tracker.ui

import java.text.NumberFormat
import java.util.Locale

fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).format(value)
fun decimal(value: Double): String = String.format(Locale.forLanguageTag("en-IN"), "%.2f", value)
