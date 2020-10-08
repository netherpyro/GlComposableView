package com.netherpyro.glcv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.netherpyro.glcv.R
import com.netherpyro.glcv.baker.EncodeTarget
import kotlinx.android.synthetic.main.d_progress.*

/**
 * @author mmikhailov on 30.04.2020.
 */
class ProgressDialog : DialogFragment() {

    companion object {
        const val CODE_REQUEST_CANCEL = "CODE_REQUEST_CANCEL"
        const val KEY_CANCEL_ACTION = "KEY_CANCEL_ACTION"
    }

    private var encodeTarget: EncodeTarget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ProgressDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.d_progress, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_cancel.setOnClickListener {
            setFragmentResult(CODE_REQUEST_CANCEL, bundleOf(KEY_CANCEL_ACTION to true))
            requireView().postDelayed({ dismiss() }, 150L)
        }
    }

    fun setProgress(encodeTarget: EncodeTarget, progress: Float) {
        if (view != null) {
            val percent = "%.0f".format(progress * 100)
                .plus("%")
            txt_progress_value.text = percent

            if (this.encodeTarget != encodeTarget) {
                this.encodeTarget = encodeTarget
                txt_encode_target.text = when (encodeTarget) {
                    EncodeTarget.VIDEO -> getString(R.string.encode_video)
                    EncodeTarget.AUDIO -> getString(R.string.encode_audio)
                }
            }
        }
    }
}