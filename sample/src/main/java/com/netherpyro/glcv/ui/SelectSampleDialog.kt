package com.netherpyro.glcv.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.netherpyro.glcv.R
import kotlinx.android.synthetic.main.d_select_sample.*

/**
 * @author mmikhailov on 30.04.2020.
 */
class SelectSampleDialog : DialogFragment() {

    companion object {
        const val SAMPLE_DIRECT = "SAMPLE_DIRECT"
        const val SAMPLE_COMPOSER = "SAMPLE_COMPOSER"
    }

    private lateinit var selectSampleListener: SelectSampleListener

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            selectSampleListener = context as SelectSampleListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement SelectSampleListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ProgressDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.d_select_sample, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_glcv.setOnClickListener { selectSample(SAMPLE_DIRECT) }
        btn_composer.setOnClickListener { selectSample(SAMPLE_COMPOSER) }
    }

    private fun selectSample(sample: String) {
        requireView().postDelayed({
            selectSampleListener.onSelected(sample)
            dismiss()
        }, 150L)
    }

    interface SelectSampleListener {
        fun onSelected(sampleKey: String)
    }
}