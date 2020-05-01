package com.netherpyro.glcv.ui

import android.Manifest
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.netherpyro.glcv.R

/**
 * @author mmikhailov on 2019-11-30.
 */
class MainActivity : AppCompatActivity(), SelectSampleDialog.SelectSampleListener {

    companion object {
        val PERMISSIONS = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            SelectSampleDialog().show(supportFragmentManager, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean("shown_dialog", true)

        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSelected(sampleKey: String) {
        val fragment = when (sampleKey) {
            SelectSampleDialog.SAMPLE_DIRECT -> GlViewFragment()
            SelectSampleDialog.SAMPLE_COMPOSER -> ComposerFragment()
            else -> throw IllegalArgumentException("Unknown sample")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}