package org.dhis2.data.biometrics

import com.google.gson.reflect.TypeToken
import org.dhis2.commons.biometrics.BiometricsIcon
import org.dhis2.commons.biometrics.BiometricsPreference
import org.dhis2.commons.prefs.BasicPreferenceProvider
import org.dhis2.usescases.biometrics.BiometricsConfig
import org.dhis2.usescases.biometrics.BiometricsConfigRepository
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit
import timber.log.Timber

class BiometricsConfigRepositoryImpl(
    private val d2: D2,
    private val preferenceProvider: BasicPreferenceProvider,
    private val biometricsConfigApi: BiometricsConfigApi
) : BiometricsConfigRepository {

    override fun sync() {
        try {
            val response = biometricsConfigApi.getData().execute()

            val configOptions = response.body()

            if (response.isSuccessful && configOptions != null) {
                preferenceProvider.saveAsJson(BiometricsPreference.CONFIGURATIONS, configOptions)
                Timber.d("BiometricsConfig synced!")

                val userOrgUnitGroups =
                    d2.organisationUnitModule().organisationUnits()
                        .byOrganisationUnitScope(OrganisationUnit.Scope.SCOPE_DATA_CAPTURE)
                        .withOrganisationUnitGroups()
                        .blockingGet().flatMap { ou ->
                            if (ou.organisationUnitGroups() != null) ou.organisationUnitGroups()!!
                                .map { ouGroup -> ouGroup.uid() }
                            else listOf()
                        }.distinct()

                preferenceProvider.saveAsJson(BiometricsPreference.USER_ORG_UNIT_GROUPS, userOrgUnitGroups)
            } else {
                Timber.e(response.errorBody()?.string())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun getUserOrgUnitGroups(): List<String> {
        val listStringType = object : TypeToken<List<String>>() {}

        return preferenceProvider.getObjectFromJson(
            BiometricsPreference.USER_ORG_UNIT_GROUPS,
            listStringType,
            listOf()
        )
    }

    override fun getBiometricsConfigs(): List<BiometricsConfig> {
        val biometricsConfigType = object : TypeToken<List<BiometricsConfig>>() {}

        return preferenceProvider.getObjectFromJson(
            BiometricsPreference.CONFIGURATIONS,
            biometricsConfigType,
            listOf()
        )
    }

    override fun saveSelectedConfig(config: BiometricsConfig) {
        preferenceProvider.setValue(BiometricsPreference.PROJECT_ID, config.projectId)

        preferenceProvider.setValue(BiometricsPreference.USER_ID, config.userId)

        preferenceProvider.setValue(
            BiometricsPreference.CONFIDENCE_SCORE_FILTER,
            config.confidenceScoreFilter ?: 0
        )

        val icon =
            BiometricsIcon.values()
                .firstOrNull { it.name == config.icon?.toUpperCase() }?.name
                ?: BiometricsIcon.FINGERPRINT.name

        preferenceProvider.setValue(BiometricsPreference.ICON, icon)
        preferenceProvider.setValue(
            BiometricsPreference.LAST_VERIFICATION_DURATION,
            config.lastVerificationDuration ?: 0
        )

        Timber.d("downloadBiometricsConfig!")
        Timber.d("orgUnitGroup: ${config.orgUnitGroup}")
        Timber.d("program: ${config.program}")
        Timber.d("projectId: ${config.projectId}")
        Timber.d("userId: ${config.userId}")
        Timber.d("confidenceScoreFilter: ${config.confidenceScoreFilter}")
        Timber.d("icon: $icon")
        Timber.d("lastVerificationDuration: ${config.lastVerificationDuration}")
    }
}