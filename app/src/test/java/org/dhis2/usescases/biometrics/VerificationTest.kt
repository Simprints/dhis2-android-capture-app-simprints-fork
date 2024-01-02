package org.dhis2.usescases.biometrics

import org.junit.Assert
import org.junit.Test
import java.util.Date

class VerificationTest {
    private val maxDuration = 30

    @Test
    fun `Should return is valid if last verification time is lower than max duration`() {
        val lastVerification = givenADatePreviousFromNow(maxDuration - 1)
        Assert.assertTrue(isLastVerificationValid(lastVerification, maxDuration, false))
    }

    @Test
    fun `Should return is valid if last verification time is equal to max duration`() {
        val lastVerification = givenADatePreviousFromNow(maxDuration)
        Assert.assertTrue(isLastVerificationValid(lastVerification, maxDuration, false))
    }

    @Test
    fun `Should return is not valid if last verification time greater than to max duration`() {
        val lastVerification = givenADatePreviousFromNow(maxDuration + 1)
        Assert.assertFalse(isLastVerificationValid(lastVerification, maxDuration, false))
    }

    private fun givenADatePreviousFromNow(minutes: Int): Date {
        val timeInSecs: Long = Date().time
        return Date(timeInSecs - minutes * 60 * 1000)
    }
}
