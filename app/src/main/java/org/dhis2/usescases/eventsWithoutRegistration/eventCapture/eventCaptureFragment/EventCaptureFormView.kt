package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.eventCaptureFragment

interface EventCaptureFormView {
    fun performSaveClick()
    fun hideSaveButton()
    fun showSaveButton()
    fun onReopen()
    fun verifyBiometrics(
        biometricsGuid: String?,
        teiOrgUnit: String?,
        trackedEntityInstanceId: String?
    )

    fun registerBiometrics(
        teiOrgUnit: String?,
        trackedEntityInstanceId: String?
    )
}
