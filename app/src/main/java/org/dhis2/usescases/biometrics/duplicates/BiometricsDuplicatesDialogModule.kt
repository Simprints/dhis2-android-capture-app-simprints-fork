package org.dhis2.usescases.biometrics.duplicates

import android.content.Context
import dagger.Module
import dagger.Provides
import dhis2.org.analytics.charts.Charts
import org.dhis2.R
import org.dhis2.commons.di.dagger.PerFragment
import org.dhis2.commons.filters.data.FilterPresenter
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.reporting.CrashReportController
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.commons.schedulers.SchedulerProvider
import org.dhis2.data.dhislogic.DhisEnrollmentUtils
import org.dhis2.data.dhislogic.DhisPeriodUtils
import org.dhis2.data.enrollment.EnrollmentUiDataHelper
import org.dhis2.data.forms.dataentry.SearchTEIRepository
import org.dhis2.data.forms.dataentry.SearchTEIRepositoryImpl
import org.dhis2.data.sorting.SearchSortingValueSetter
import org.dhis2.ui.ThemeManager
import org.dhis2.usescases.searchTrackEntity.SearchRepository
import org.dhis2.usescases.searchTrackEntity.SearchRepositoryImpl
import org.dhis2.utils.DateUtils
import org.hisp.dhis.android.core.D2

@Module
class BiometricsDuplicatesDialogModule(private val context: Context, private val teiType: String,
    private val initialProgram: String) {

    @Provides
    fun enrollmentUiDataHelper(context: Context): EnrollmentUiDataHelper {
        return EnrollmentUiDataHelper(context)
    }

    @Provides
    fun searchSortingValueSetter(
        context: Context,
        d2: D2,
        enrollmentUiDataHelper: EnrollmentUiDataHelper
    ): SearchSortingValueSetter {
        val unknownLabel = context.getString(R.string.unknownValue)
        val eventDateLabel = context.getString(R.string.most_recent_event_date)
        val enrollmentStatusLabel = context.getString(R.string.filters_title_enrollment_status)
        val enrollmentDateDefaultLabel = context.getString(R.string.enrollment_date)
        val uiDateFormat = DateUtils.SIMPLE_DATE_FORMAT
        return SearchSortingValueSetter(
            d2,
            unknownLabel,
            eventDateLabel,
            enrollmentStatusLabel,
            enrollmentDateDefaultLabel,
            uiDateFormat,
            enrollmentUiDataHelper
        )
    }

    @Provides
    internal fun searchRepository(d2: D2): SearchTEIRepository {
        return SearchTEIRepositoryImpl(d2, DhisEnrollmentUtils(d2))
    }

    @Provides
    fun searchRepository(
        d2: D2, filterPresenter: FilterPresenter?,
        resources: ResourceManager?,
        searchSortingValueSetter: SearchSortingValueSetter?,
        periodUtils: DhisPeriodUtils?, charts: Charts?,
        crashReportController: CrashReportController?,
        networkUtils: NetworkUtils?,
        searchTEIRepository: SearchTEIRepository?,
        themeManager: ThemeManager?
    ): SearchRepository {
        return SearchRepositoryImpl(
            teiType,
            initialProgram,
            d2,
            filterPresenter,
            resources,
            searchSortingValueSetter,
            periodUtils,
            charts,
            crashReportController,
            networkUtils,
            searchTEIRepository,
            themeManager
        )
    }

    @Provides
    fun providesPresenter(
        d2: D2,
        searchRepository: SearchRepository,
        schedulerProvider: SchedulerProvider
    ): BiometricsDuplicatesDialogPresenter {
        return BiometricsDuplicatesDialogPresenter(d2, searchRepository, schedulerProvider)
    }
}
