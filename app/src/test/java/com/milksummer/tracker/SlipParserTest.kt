package com.milksummer.tracker

import com.milksummer.tracker.domain.model.compareSlip
import com.milksummer.tracker.ui.ocr.*
import org.junit.Assert.*
import org.junit.Test

class SlipParserTest {
    @Test fun cleanSlipExtractsAllValues() {
        assertEquals(ParsedSlip(5.2, 6.5, 8.8, 38.5, 200.2), SlipParser.parse("Qty: 5.2\nFAT: 6.5\nSNF: 8.8\nRate: 38.50\nTotal: 200.20"))
    }

    @Test fun missingFieldsStayBlankRatherThanTakingNextLabelValue() {
        assertEquals(ParsedSlip(quantity = 5.2, snf = 8.8, rate = 38.5), SlipParser.parse("Quantity: 5.2\nFAT:\nSNF: 8.8\nRate: 38.5"))
    }

    @Test fun sameLineAndTabularHeadingsBothWork() {
        val expected = ParsedSlip(5.2, 6.5, 8.8, 38.5, 200.2)
        assertEquals(expected, SlipParser.parse("Qty 5.2 FAT 6.5 SNF 8.8 Rate 38.5 Total 200.2"))
        assertEquals(expected, SlipParser.parse("Qty FAT SNF Rate Total\n5.2 6.5 8.8 38.5 200.2"))
    }

    @Test fun hindiLabelsAndDigitsParse() {
        assertEquals(ParsedSlip(quantity = 5.2, fat = 6.5, rate = 38.5, total = 200.2), SlipParser.parse("मात्रा: ५.२\nवसा: ६.५\nदर: ३८.५०\nकुल: २००.२०"))
    }

    @Test fun numberOnFollowingLineAndCommaFormatsParse() {
        assertEquals(ParsedSlip(quantity = 5.2, total = 1200.2), SlipParser.parse("Qty\n5,2\nTotal: 1,200.20"))
    }

    @Test fun emptyOrUnrelatedTextProducesNoValues() {
        assertTrue(SlipParser.parse("").isEmpty)
        assertTrue(SlipParser.parse("Dairy 12345\n2026-10-24\nMember 9").isEmpty)
    }

    @Test fun unitsInRateLabelDoNotBecomeQuantity() {
        assertEquals(ParsedSlip(rate = 38.5), SlipParser.parse("Rate per liter: 38.50"))
    }

    @Test fun mismatchThresholdUsesExactDecimalComparison() {
        assertTrue(compareSlip(5.2, 38.5, 200.2)!!.matches)
        assertTrue(compareSlip(5.2, 38.5, 200.7)!!.matches)
        assertFalse(compareSlip(5.2, 38.5, 200.71)!!.matches)
        assertEquals(80.0, compareSlip(5.2, 38.5, 280.2)!!.difference.toDouble(), 0.000001)
        assertNull(compareSlip(Double.NaN, 38.5, 200.2))
    }
}
