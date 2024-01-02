package org.dhis2.usescases.eventsWithoutRegistration.eventCapture.eventCaptureFragment;

import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_ENROLL_REQUEST;
import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_TRACKED_ENTITY_INSTANCE_ID;
import static org.dhis2.commons.extensions.ViewExtensionsKt.closeKeyboard;
import static org.dhis2.utils.granularsync.SyncStatusDialogNavigatorKt.OPEN_ERROR_LOCATION;

import static android.app.Activity.RESULT_OK;

import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_GUID;
import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_TEI_ORGANISATION_UNIT;
import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_VERIFICATION_STATUS;
import static org.dhis2.commons.biometrics.BiometricConstantsKt.BIOMETRICS_VERIFY_REQUEST;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentTransaction;


import org.dhis2.R;
import org.dhis2.commons.Constants;
import org.dhis2.data.biometrics.BiometricsClient;
import org.dhis2.data.biometrics.BiometricsClientFactory;
import org.dhis2.data.biometrics.RegisterResult;
import org.dhis2.data.biometrics.VerifyResult;
import org.dhis2.databinding.SectionSelectorFragmentBinding;
import org.dhis2.form.model.EventRecords;
import org.dhis2.form.ui.FormView;
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity;
import org.dhis2.usescases.general.FragmentGlobalAbstract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import javax.inject.Inject;

import kotlin.Unit;

public class EventCaptureFormFragment extends FragmentGlobalAbstract implements EventCaptureFormView,
        OnEditionListener {

    @Inject
    EventCaptureFormPresenter presenter;

    private EventCaptureActivity activity;
    private SectionSelectorFragmentBinding binding;
    private FormView formView;

    private String biometricsGuid;
    private int biometricsVerificationStatus;
    private String teiOrgUnit;

    private String trackedEntityInstanceId;

    public static EventCaptureFormFragment newInstance(String eventUid, Boolean openErrorSection) {
        EventCaptureFormFragment fragment = new EventCaptureFormFragment();
        Bundle args = new Bundle();
        args.putString(Constants.EVENT_UID, eventUid);
        args.putBoolean(OPEN_ERROR_LOCATION, openErrorSection);
        fragment.setArguments(args);
        return fragment;
    }

    public static EventCaptureFormFragment newInstance(
            String eventUid,
            Boolean openErrorSection,
            String guid, int status,
            String teiOrgUnit,
            String trackedEntityInstanceId) {
        EventCaptureFormFragment fragment = new EventCaptureFormFragment();
        Bundle args = new Bundle();
        args.putString(Constants.EVENT_UID, eventUid);
        args.putBoolean(OPEN_ERROR_LOCATION, openErrorSection);
        args.putString(BIOMETRICS_GUID, guid);
        args.putInt(BIOMETRICS_VERIFICATION_STATUS, status);
        args.putString(BIOMETRICS_TEI_ORGANISATION_UNIT, teiOrgUnit);
        args.putString(BIOMETRICS_TRACKED_ENTITY_INSTANCE_ID, trackedEntityInstanceId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.activity = (EventCaptureActivity) context;

        biometricsGuid = getArguments().getString(BIOMETRICS_GUID);
        biometricsVerificationStatus = getArguments().getInt(BIOMETRICS_VERIFICATION_STATUS);
        teiOrgUnit = getArguments().getString(BIOMETRICS_TEI_ORGANISATION_UNIT);
        trackedEntityInstanceId =  getArguments().getString(BIOMETRICS_TRACKED_ENTITY_INSTANCE_ID);

        activity.eventCaptureComponent.plus(
                new EventCaptureFormModule(
                        this,
                        getArguments().getString(Constants.EVENT_UID))
        ).inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        formView = new FormView.Builder()
                .locationProvider(locationProvider)
                .onLoadingListener(loading -> {
                    if (loading) {
                        activity.showProgress();
                    } else {
                        activity.hideProgress();
                    }
                    return Unit.INSTANCE;
                })
                .onFocused(() -> {
                    activity.hideNavigationBar();
                    return Unit.INSTANCE;
                })
                .onPercentageUpdate(percentage -> {
                    activity.updatePercentage(percentage);
                    return Unit.INSTANCE;
                }).onItemChangeListener(rowAction ->{
                    activity.refreshProgramStageName();
                    return Unit.INSTANCE;
                }).onDataIntegrityResult(result -> {
                    presenter.handleDataIntegrityResult(result);
                    return Unit.INSTANCE;
                })
                .factory(activity.getSupportFragmentManager())
                .setRecords(new EventRecords(getArguments().getString(Constants.EVENT_UID)))
                .openErrorLocation(getArguments().getBoolean(OPEN_ERROR_LOCATION, false))
                .onFieldsLoadingListener ( fields -> presenter.onFieldsLoading(fields))
                .build();
        activity.setFormEditionListener(this);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.section_selector_fragment, container, false);
        binding.setPresenter(activity.getPresenter());
        binding.actionButton.setOnClickListener(view -> {
            closeKeyboard(view);
            performSaveClick();
        });

        presenter.initBiometricsValues(biometricsGuid, biometricsVerificationStatus, teiOrgUnit, trackedEntityInstanceId);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.formViewContainer, formView).commit();
        formView.setScrollCallback(isSectionVisible -> {
            animateFabButton(isSectionVisible);
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.showOrHideSaveButton();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BIOMETRICS_VERIFY_REQUEST: {
                if (resultCode == RESULT_OK) {
                    VerifyResult result = BiometricsClientFactory.INSTANCE.get(
                            this.getContext()).handleVerifyResponse(data);


                    if (result instanceof VerifyResult.Match) {
                        presenter.refreshBiometricsStatus(1, true, null);
                    } else if (result instanceof VerifyResult.NoMatch) {
                        presenter.refreshBiometricsStatus(0, true,null);
                    } else if (result instanceof VerifyResult.Failure) {
                        presenter.refreshBiometricsStatus(0, true,null);
                    }
                }
                break;
            }
            case BIOMETRICS_ENROLL_REQUEST: {
                if (data != null) {
                    RegisterResult result = BiometricsClientFactory.INSTANCE.get(
                            this.getContext()).handleRegisterResponse(data);


                    if (result instanceof RegisterResult.Completed) {
                        presenter.refreshBiometricsStatus(1, true,((RegisterResult.Completed) result).getGuid());
                    } else if (result instanceof RegisterResult.Failure) {
                        presenter.refreshBiometricsStatus(0, true, null);
                    } else if (result instanceof RegisterResult.PossibleDuplicates) {
                   /*     presenter.onBiometricsPossibleDuplicates(
                                result.guids,
                                result.sessionId
                        )*/
                    }
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void animateFabButton(boolean sectionIsVisible) {
        int translationX = 1000;
        if (sectionIsVisible) translationX = 0;

        binding.actionButton.animate().translationX(translationX).setDuration(500).start();
    }

    @Override
    public void performSaveClick() {
        formView.onSaveClick();
    }

    @Override
    public void onEditionListener() {
        formView.onEditionFinish();
    }

    @Override
    public void hideSaveButton() {
        binding.actionButton.setVisibility(View.GONE);
    }

    @Override
    public void showSaveButton() {
        binding.actionButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onReopen() {
        formView.reload();
    }

    @Override
    public void verifyBiometrics(@Nullable String biometricsGuid, @Nullable String teiOrgUnit, @Nullable String trackedEntityInstanceId) {
        HashMap extras = new HashMap<>();
        extras.put(BiometricsClient.SIMPRINTS_TRACKED_ENTITY_INSTANCE_ID, trackedEntityInstanceId);

        BiometricsClientFactory.INSTANCE.get(this.getContext()).verify(this, biometricsGuid, teiOrgUnit, extras);
    }

    @Override
    public void registerBiometrics(@Nullable String teiOrgUnit, @Nullable String trackedEntityInstanceId) {
        HashMap extras = new HashMap<>();
        extras.put(BiometricsClient.SIMPRINTS_TRACKED_ENTITY_INSTANCE_ID, trackedEntityInstanceId);

        BiometricsClientFactory.INSTANCE.get(this.getContext()).registerFromFragment(this, teiOrgUnit, extras);
    }
}