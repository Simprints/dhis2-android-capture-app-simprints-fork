package org.dhis2.data.service

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerService
import org.dhis2.commons.prefs.BasicPreferenceProvider
import org.dhis2.commons.prefs.PreferenceProvider
import org.dhis2.data.biometrics.BiometricsConfigApi
import org.dhis2.data.biometrics.BiometricsConfigRepositoryImpl
import org.dhis2.data.service.workManager.WorkManagerController
import org.dhis2.usescases.biometrics.BiometricsConfigRepository
import org.dhis2.utils.analytics.AnalyticsHelper
import org.hisp.dhis.android.core.D2

@Module
class SyncDataWorkerModule {
    @Provides
    @PerService
    fun syncRepository(d2: D2): SyncRepository {
        return SyncRepositoryImpl(d2)
    }

    @Provides
    @PerService
    fun biometricsConfigRepository(
        d2: D2,
        basicPreferences: BasicPreferenceProvider
    ): BiometricsConfigRepository {
        val biometricsConfigApi = d2.retrofit().create(
            BiometricsConfigApi::class.java
        )
        return BiometricsConfigRepositoryImpl(d2, basicPreferences, biometricsConfigApi)
    }

    @Provides
    @PerService
    internal fun syncPresenter(
        d2: D2,
        preferences: PreferenceProvider,
        workManagerController: WorkManagerController,
        analyticsHelper: AnalyticsHelper,
        syncStatusController: SyncStatusController,
        syncRepository: SyncRepository,
        biometricsConfigRepository: BiometricsConfigRepository

    ): SyncPresenter {
        return SyncPresenterImpl(
            d2,
            preferences,
            workManagerController,
            analyticsHelper,
            syncStatusController,
            syncRepository,
            biometricsConfigRepository
        )
    }
}
