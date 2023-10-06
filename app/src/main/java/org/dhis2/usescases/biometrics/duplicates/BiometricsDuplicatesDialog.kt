package org.dhis2.usescases.biometrics.duplicates

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.google.android.material.snackbar.Snackbar
import io.reactivex.functions.Consumer
import org.dhis2.Bindings.app
import org.dhis2.R
import org.dhis2.commons.biometrics.BIOMETRICS_CONFIRM_IDENTITY_REQUEST
import org.dhis2.commons.data.SearchTeiModel
import org.dhis2.data.biometrics.BiometricsClient
import org.dhis2.data.biometrics.BiometricsClientFactory.get
import org.dhis2.databinding.DialogBiometricsDuplicatesBinding
import org.dhis2.utils.LastSelection
import org.hisp.dhis.android.core.arch.call.D2Progress
import javax.inject.Inject

class BiometricsDuplicatesDialog : DialogFragment(), BiometricsDuplicatesDialogView {

    private var onEnrollNewListener: ((sessionId: String) -> Unit)? = null
    private var onOpenTeiDashboardListener: ((String, String, String) -> Unit)? = null
    private lateinit var binding: DialogBiometricsDuplicatesBinding
    private var lastSelection: LastSelection? = null

    @Inject
    lateinit var presenter: BiometricsDuplicatesDialogPresenter

    private lateinit var adapter: BiometricsDuplicatesDialogAdapter

    var isDialogShown = false
        private set

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        create(
            requireArguments().getString(TRACKED_ENTITY_TYPE_UID)!!,
            requireArguments().getString(PROGRAM_UID)!!
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_biometrics_duplicates,
            container,
            false
        )

        val biometricsGuids: List<String> =
            requireArguments().getString(BIOMETRICS_GUIDs)!!.trim().split(",")
        val biometricsSessionId = requireArguments().getString(BIOMETRICS_SESSION_ID)!!
        val programUid = requireArguments().getString(PROGRAM_UID)!!
        val trackedEntityTypeUid = requireArguments().getString(TRACKED_ENTITY_TYPE_UID)!!
        val biometricsAttributeUid = requireArguments().getString(BIOMETRICS_ATTRIBUTE_UID)!!

        binding.enrollNewButton.setOnClickListener {
            presenter.enrollNewClick()
        }
        binding.cancelButton.setOnClickListener { this.dismiss() }

        this.adapter =
            BiometricsDuplicatesDialogAdapter(activity?.supportFragmentManager) { searchTeiModel ->

                presenter.onTEIClick(
                    searchTeiModel.tei.uid(),
                    searchTeiModel.selectedEnrollment.uid(),
                    searchTeiModel.isOnline
                )
            }

        binding.duplicatesRecycler.adapter = adapter

        presenter.init(
            this,
            biometricsGuids,
            biometricsSessionId,
            programUid,
            trackedEntityTypeUid,
            biometricsAttributeUid
        )

        return binding.root
    }

    override fun onCancel(dialog: DialogInterface) {
        presenter.onDetach()
        super.onCancel(dialog)
    }

    override fun setLiveData(liveData: LiveData<PagedList<SearchTeiModel>>) {
        liveData.observe(this, Observer { searchTeiModels: PagedList<SearchTeiModel> ->
            if (searchTeiModels.size == 0) {
                binding.duplicatesEmptyContainer.visibility = View.VISIBLE
            } else {
                binding.duplicatesEmptyContainer.visibility = View.GONE
                adapter.submitList(searchTeiModels)
            }
        })
    }

    override fun openDashboard(teiUid: String, programUid: String, enrollmentUid: String) {
        onOpenTeiDashboardListener?.invoke(teiUid, programUid, enrollmentUid)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        isDialogShown = true
        super.show(manager, tag)
    }

    override fun dismiss() {
        presenter.onDetach()
        if (isDialogShown) {
            isDialogShown = false
            super.dismiss()
        }
    }

    override fun downloadProgress(): Consumer<D2Progress> {
        return Consumer {
            Snackbar.make(
                binding.root,
                getString(R.string.downloading),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun couldNotDownload(typeName: String) {
        Toast.makeText(
            context,
            getString(R.string.download_tei_error, typeName),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun enrollNew(biometricsSessionId: String) {
        onEnrollNewListener?.invoke(biometricsSessionId)
        dismiss()
    }

    override fun sendBiometricsConfirmIdentity(
        sessionId: String,
        guid: String,
        teiUid: String,
        enrollmentUid: String,
        isOnline: Boolean
    ) {
        lastSelection = LastSelection(teiUid, enrollmentUid, isOnline)

        val extras =
            mutableMapOf(BiometricsClient.SIMPRINTS_TRACKED_ENTITY_INSTANCE_ID to teiUid)

        context?.let { get(it).confirmIdentify(this, sessionId, guid, extras) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BIOMETRICS_CONFIRM_IDENTITY_REQUEST -> {
                if (lastSelection != null) {
                    presenter.onTEIClick(
                        lastSelection!!.teiUid, lastSelection!!.enrollmentUid,
                        lastSelection!!.isOnline
                    )
                    lastSelection = null
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun setOnOpenTeiDashboardListener(onOpenTeiDashboardListener: (teiUid: String, programUid: String, enrollmentUid: String) -> Unit) {
        this.onOpenTeiDashboardListener = onOpenTeiDashboardListener
    }

    fun setOnEnrollNewListener(onEnrollNewListener: (sessionId: String) -> Unit) {
        this.onEnrollNewListener = onEnrollNewListener
    }

    private fun create(teiType: String, program: String) {
        app()
            .userComponent()!!
            .plus(BiometricsDuplicatesDialogModule(requireContext(), teiType, program))
            .inject(this)
    }

    companion object {
        private const val BIOMETRICS_GUIDs = "GUIDs"
        private const val BIOMETRICS_SESSION_ID = "BIOMETRICS_SESSION_ID"
        private const val PROGRAM_UID = "PROGRAM_UID"
        private const val TRACKED_ENTITY_TYPE_UID = "TRACKED_ENTITY_TYPE_UID"
        private const val BIOMETRICS_ATTRIBUTE_UID = "BIOMETRICS_ATTRIBUTE_UID"

        val TAG: String = this::class.java.name

        @JvmStatic
        fun newInstance(
            guids: List<String>,
            sessionId: String,
            programUid: String,
            trackedEntityTypeUid: String,
            biometricsAttributeUid: String
        ): BiometricsDuplicatesDialog {
            val fragment = BiometricsDuplicatesDialog()

            val args = Bundle()
            args.putString(BIOMETRICS_GUIDs, guids.joinToString(","))
            args.putString(BIOMETRICS_SESSION_ID, sessionId)
            args.putString(PROGRAM_UID, programUid)
            args.putString(TRACKED_ENTITY_TYPE_UID, trackedEntityTypeUid)
            args.putString(BIOMETRICS_ATTRIBUTE_UID, biometricsAttributeUid)
            fragment.arguments = args

            return fragment
        }
    }
}
