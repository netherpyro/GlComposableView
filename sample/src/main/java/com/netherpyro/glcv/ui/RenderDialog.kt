package com.netherpyro.glcv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.netherpyro.glcv.R
import kotlinx.android.synthetic.main.d_render.*

/**
 * @author mmikhailov on 02.05.2020.
 */
class RenderDialog : DialogFragment() {

    companion object {
        const val CODE_REQUEST_RENDER = "CODE_REQUEST_RENDER"
        const val KEY_FPS = "KEY_FPS"
        const val KEY_SIDE_SIZE = "KEY_SIDE_SIZE"
        const val KEY_USE_SERVICE = "KEY_USE_SERVICE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ProgressDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.d_render, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_start.setOnClickListener {
            dismiss()
            setFragmentResult(CODE_REQUEST_RENDER, bundleOf(
                    KEY_FPS to rg_fps.checkedRadioButtonId.toFps(),
                    KEY_SIDE_SIZE to rg_resolution.checkedRadioButtonId.toSideMinWidth(),
                    KEY_USE_SERVICE to chb_service.isChecked
            ))
        }
    }

    private fun Int.toFps() = when (this) {
        R.id.rb_30fps -> 30
        R.id.rb_60fps -> 60
        else -> 24
    }

    private fun Int.toSideMinWidth() = when (this) {
        R.id.rb_1080 -> 1080
        R.id.rb_2160 -> 2160
        else -> 720
    }
}