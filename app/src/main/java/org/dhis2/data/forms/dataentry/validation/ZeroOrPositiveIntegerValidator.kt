package org.dhis2.data.forms.dataentry.validation

import org.dhis2.utils.Validator

class ZeroOrPositiveIntegerValidator : Validator {

    override fun validate(text: String) = text.toIntOrNull()?.let {
        text.matches(regex)
    } ?: false

    companion object {
        val regex = Regex("^[0-9]*\$")
    }
}
