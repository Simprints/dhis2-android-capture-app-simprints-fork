package org.dhis2.usescases.biometrics

interface BiometricsConfigRepository {
    fun sync()
    fun getUserOrgUnitGroups(): List<String>
    fun getBiometricsConfigs(): List<BiometricsConfig>
    fun saveSelectedConfig(config: BiometricsConfig)
}