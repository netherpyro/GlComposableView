package com.netherpyro.glcv.ui.contract

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts

/**
 * @author mmikhailov on 01.05.2020.
 */
class GetMultipleMedia : ActivityResultContracts.GetMultipleContents() {

    override fun createIntent(context: Context, input: String): Intent {
        super.createIntent(context, input)
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            .setType(input)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
}