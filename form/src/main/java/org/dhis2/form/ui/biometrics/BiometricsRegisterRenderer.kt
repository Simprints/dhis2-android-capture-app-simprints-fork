package org.dhis2.form.ui.biometrics

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import org.dhis2.commons.biometrics.getBioIconNew
import org.dhis2.commons.biometrics.getBioIconSuccess
import org.dhis2.commons.biometrics.getBioIconWarning
import org.dhis2.form.R
import org.dhis2.form.model.biometrics.BiometricsRegistrationUIModel
import timber.log.Timber

class BiometricsRegisterRenderer(private val binding: ViewDataBinding,private val uiModel: BiometricsRegistrationUIModel) {
    private val biometricsButton: LinearLayout = binding.root.findViewById(R.id.biometrics_button)
    private val retakeButton: LinearLayout =
        binding.root.findViewById(R.id.biometrics_retake_button)
    private val biometricsButtonText: TextView =
        binding.root.findViewById(R.id.biometrics_button_text)
    private val biometricsButtonIcon: ImageView =
        binding.root.findViewById(R.id.biometrics_button_icon)

    fun onInitial() {
        Timber.tag("BiometricsView").d("onInitial");

        retakeButton.visibility = View.GONE;
        biometricsButton.isEnabled = true;
        biometricsButtonIcon.setImageDrawable(
            AppCompatResources.getDrawable(
                binding.root.context,
                binding.root.context.getBioIconNew()
            )
        )

        biometricsButton.setOnClickListener { uiModel.onBiometricsClick() }
    }

    fun onSuccess() {
        Timber.tag("BiometricsView").d("onSuccess");

        retakeButton.visibility = View.VISIBLE;
        biometricsButton.background = ContextCompat.getDrawable(
            binding.root.context,
            R.drawable.button_round_success
        );

        biometricsButton.isEnabled = false;

        biometricsButtonText.setText(R.string.biometrics_completed);

        biometricsButtonIcon.setImageDrawable(
            AppCompatResources.getDrawable(
                binding.root.context,
                binding.root.context.getBioIconSuccess()
            )
        )
    }


    fun onFailure() {
        retakeButton.setOnClickListener { uiModel.onBiometricsClick() }

        Timber.tag("BiometricsView").d("onFailure");
        retakeButton.visibility = View.VISIBLE;
        biometricsButton.background = ContextCompat.getDrawable(
            binding.root.context,
            R.drawable.button_round_warning
        );

        biometricsButton.isEnabled = false;

        biometricsButtonText.setText(R.string.biometrics_declined);

        biometricsButtonIcon.setImageDrawable(
            AppCompatResources.getDrawable(
                binding.root.context,
                binding.root.context.getBioIconWarning()
            )
        )
    }
}