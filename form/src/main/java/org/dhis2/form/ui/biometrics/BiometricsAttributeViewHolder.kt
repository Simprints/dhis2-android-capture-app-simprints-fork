package org.dhis2.form.ui.biometrics

import androidx.databinding.ViewDataBinding
import org.dhis2.commons.biometrics.BIOMETRICS_FAILURE_PATTERN
import org.dhis2.form.model.FieldUiModel
import org.dhis2.form.model.biometrics.BiometricsRegistrationUIModel
import org.dhis2.form.ui.FormViewHolder
import org.dhis2.form.ui.event.RecyclerViewUiEvents
import org.dhis2.form.ui.intent.FormIntent
import timber.log.Timber

class BiometricsAttributeViewHolder(private val binding: ViewDataBinding) :
    FormViewHolder(binding) {


    fun bind(
        uiModel: FieldUiModel,
        callback: FieldItemCallback,
    ) {
        val itemCallback: FieldUiModel.Callback = object : FieldUiModel.Callback {
            override fun recyclerViewUiEvents(uiEvent: RecyclerViewUiEvents) {
                callback.recyclerViewEvent(uiEvent)
            }

            override fun intent(intent: FormIntent) {
                var formIntent = intent
                if (intent is FormIntent.OnNext) {
                    formIntent = intent.copy(
                        position = layoutPosition
                    )
                }
                callback.intent(formIntent)
            }
        }
        uiModel.setCallback(itemCallback)

        val renderer = BiometricsRegisterRenderer(binding, uiModel as BiometricsRegistrationUIModel)

        val value = uiModel.value

        Timber.tag("BiometricsView value").d(value)

        if (!value.isNullOrEmpty()) {
            if (value.startsWith(BIOMETRICS_FAILURE_PATTERN)) {
                renderer.onFailure()
            } else {
                renderer.onSuccess()
            }
        } else {
            renderer.onInitial()
        }
    }
}