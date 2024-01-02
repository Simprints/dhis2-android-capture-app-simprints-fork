package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.eventCaptureFragment

import org.dhis2.form.data.DataIntegrityCheckResult
import org.dhis2.form.data.FieldsWithErrorResult
import org.dhis2.form.data.FieldsWithWarningResult
import org.dhis2.form.data.MissingMandatoryResult
import org.dhis2.form.data.NotSavedResult
import org.dhis2.form.data.SuccessfulResult
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.biometrics.BiometricsDataElementStatus
import org.dhis2.form.model.biometrics.BiometricsDataElementUiModelImpl
import org.dhis2.usescases.biometrics.BIOMETRICS_ENABLED
import org.dhis2.usescases.biometrics.isLastVerificationValid
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureContract
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.event.EventEditableStatus
import org.jetbrains.annotations.Nullable

class EventCaptureFormPresenter(
    private val view: EventCaptureFormView,
    private val activityPresenter: EventCaptureContract.Presenter,
    private val d2: D2,
    private val eventUid: String
) {

    private var biometricsVerificationStatus: Int = -1
    private var biometricsGuid: String? = null
    private var teiOrgUnit: String? = null
    private var trackedEntityInstanceId: String? = null

    private var biometricsVerificationUiModel: BiometricsDataElementUiModelImpl? = null


    fun handleDataIntegrityResult(result: DataIntegrityCheckResult) {
        when (result) {
            is FieldsWithErrorResult -> activityPresenter.attemptFinish(
                result.canComplete,
                result.onCompleteMessage,
                result.fieldUidErrorList,
                result.mandatoryFields,
                result.warningFields
            )

            is FieldsWithWarningResult -> activityPresenter.attemptFinish(
                result.canComplete,
                result.onCompleteMessage,
                emptyList(),
                emptyMap(),
                result.fieldUidWarningList
            )

            is MissingMandatoryResult -> activityPresenter.attemptFinish(
                result.canComplete,
                result.onCompleteMessage,
                result.errorFields,
                result.mandatoryFields,
                result.warningFields
            )

            is SuccessfulResult -> activityPresenter.attemptFinish(
                result.canComplete,
                result.onCompleteMessage,
                emptyList(),
                emptyMap(),
                emptyList()
            )

            NotSavedResult -> {
                // Nothing to do in this case
            }
        }
    }

    fun onFieldsLoading(fields: List<FieldUiModel>): List<FieldUiModel> {
        val updatedFields = updateBiometricsField(fields)

        if (BIOMETRICS_ENABLED) {

            biometricsVerificationUiModel = updatedFields.firstOrNull {
                it is BiometricsDataElementUiModelImpl
            }?.let { it as BiometricsDataElementUiModelImpl }


            biometricsVerificationUiModel?.setBiometricsRetryListener(
                object : BiometricsDataElementUiModelImpl.BiometricsReTryOnClickListener {
                    override fun onRetryClick() {
                        view.verifyBiometrics(biometricsGuid, teiOrgUnit, trackedEntityInstanceId)
                    }
                }
            )

            biometricsVerificationUiModel?.setBiometricsRegisterListener(
                object : BiometricsDataElementUiModelImpl.BiometricsRegisterClickListener {
                    override fun onClick() {
                        view.registerBiometrics(teiOrgUnit, trackedEntityInstanceId)
                    }
                }
            )

            if (biometricsVerificationUiModel != null) {
                launchBiometricsVerificationIfRequired(updatedFields)
            }
        }

        return updatedFields
    }

    private fun updateBiometricsField(fields: List<FieldUiModel>?): MutableList<FieldUiModel> {
        return fields?.map {
            if (it is BiometricsDataElementUiModelImpl) {
                val biometricsUiModel = it as BiometricsDataElementUiModelImpl
                val status = mapVerificationStatus(biometricsVerificationStatus)
                biometricsUiModel
                    .setValue(biometricsGuid)
                    .setStatus(status)
            } else {
                it
            }
        } as MutableList<FieldUiModel>
    }

    private fun launchBiometricsVerificationIfRequired(fields: List<FieldUiModel>) {
        biometricsVerificationUiModel = fields.firstOrNull {
            it is BiometricsDataElementUiModelImpl
        }?.let { it as BiometricsDataElementUiModelImpl }

        if (biometricsVerificationUiModel != null && this.biometricsGuid != null &&
            biometricsVerificationUiModel!!.status == BiometricsDataElementStatus.NOT_DONE
        ) {
            val lastUpdated = activityPresenter.getBiometricsAttributeValueInTeiLastUpdated(
                biometricsVerificationUiModel?.uid
            )

            if (!isLastVerificationValid(
                    lastUpdated,
                    activityPresenter.lastBiometricsVerificationDuration,
                    true
                )
            ) {
                view.verifyBiometrics(
                    this.biometricsGuid,
                    this.teiOrgUnit,
                    this.trackedEntityInstanceId
                )
            } else {
                refreshBiometricsStatus(1, false)
            }
        }
    }

    fun showOrHideSaveButton() {
        val isEditable =
            d2.eventModule().eventService().getEditableStatus(eventUid = eventUid).blockingGet()
        if (isEditable is EventEditableStatus.Editable) {
            view.showSaveButton()
        } else {
            view.hideSaveButton()
        }
    }

    fun <E> Iterable<E>.updated(index: Int, elem: E): List<E> =
        mapIndexed { i, existing -> if (i == index) elem else existing }

    fun initBiometricsValues(
        biometricsGuid: @Nullable String?,
        biometricsVerificationStatus: Int,
        teiOrgUnit: @Nullable String?,
        trackedEntityInstanceId: @Nullable String?
    ) {
        this.biometricsGuid = biometricsGuid
        this.biometricsVerificationStatus = biometricsVerificationStatus
        this.teiOrgUnit = teiOrgUnit
        this.trackedEntityInstanceId = trackedEntityInstanceId
    }

    fun refreshBiometricsStatus(
        biometricsVerificationStatus: Int,
        updateBiometricsGuidInAttribute: Boolean = true,
        newBiometricsGuid: String? = null
    ) {
        if (newBiometricsGuid != null){
            biometricsGuid = newBiometricsGuid
        }

        val status = mapVerificationStatus(biometricsVerificationStatus)

        if (status == BiometricsDataElementStatus.SUCCESS && updateBiometricsGuidInAttribute) {
            activityPresenter.updateBiometricsAttributeValueInTei(biometricsGuid)
        }

        this.biometricsVerificationStatus = biometricsVerificationStatus

        view.onReopen()
    }

    private fun mapVerificationStatus(biometricsVerificationStatus: Int): BiometricsDataElementStatus {
        return when (biometricsVerificationStatus) {
            0 -> BiometricsDataElementStatus.FAILURE
            1 -> BiometricsDataElementStatus.SUCCESS
            else -> BiometricsDataElementStatus.NOT_DONE
        }
    }
}
