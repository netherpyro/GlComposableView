package com.netherpyro.glcv.ui

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.netherpyro.glcv.AspectRatio
import com.netherpyro.glcv.R
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.alsoOnLaid
import com.netherpyro.glcv.baker.BakeProgressReceiver
import com.netherpyro.glcv.baker.Cancellable
import com.netherpyro.glcv.baker.renderToVideoFile
import com.netherpyro.glcv.baker.renderToVideoFileInSeparateProcess
import com.netherpyro.glcv.compose.Composer
import com.netherpyro.glcv.touches.LayerTouchListener
import com.netherpyro.glcv.ui.contract.GetMultipleMedia
import kotlinx.android.synthetic.main.f_compose.*
import java.io.File

/**
 * @author mmikhailov on 30.04.2020.
 */
class ComposerFragment : Fragment() {

    companion object {
        private const val TAG = "ComposerFragment"
        private const val TAG_PROGRESS_DIALOG = "TAG_PROGRESS_DIALOG"
        private const val KEY_USE_RECEIVER = "KEY_USE_RECEIVER"
        private const val KEY_START_TIME = "KEY_START_TIME"

        private var bakeProcess: Cancellable? = null
    }

    private val composer = Composer()
    private val transformableList = mutableListOf<Transformable>()

    private val getMedia = registerForActivityResult(GetMultipleMedia()) {
        it?.forEach { uri ->
            composer.addMedia(uri.toString(), uri) { transformable -> transformableList.add(transformable) }
        }
    }

    private val checkPermissionsAndGetMedia = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.values.fold(false) { acc, granted -> acc or granted }
        if (granted) getMedia("image/* video/*")
        else {
            val redirectToSettings = permissionsResult.keys.fold(false) { acc, permission ->
                acc or shouldShowRequestPermissionRationale(permission).not()
            }
            if (redirectToSettings) {
                startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setData(Uri.fromParts("package", requireContext().packageName, null))
                )
            }
        }
    }

    private var progressReceiver: BakeProgressReceiver? = null
    private var progressDialog: ProgressDialog? = null
    private var useReceiver = false
    private var startTimeNsec: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(ProgressDialog.CODE_REQUEST_CANCEL, this) { _, _ ->
            bakeProcess?.cancel()
            bakeProcess = null

            progressDialog = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.f_compose, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        glView.enableGestures = true
        composer.setViewportColor(Color.CYAN)
        composer.setBaseColor(Color.YELLOW)
        composer.setAspectRatio(16 / 9f)
        composer.setGlView(glView)

        // tiger
        /*composer.addVideo(
                "video3",
                Uri.parse("content://media/external/file/3370"),
                trimmedDuration = 2000L
        ) { transformable -> transformableList.add(transformable) }*/

        /*composer.addImage(
                "image1",
                Uri.parse("content://media/external/file/129"),
                startMs = 1000L
        ) { transformable -> transformableList.add(transformable) }

        composer.addImage(
                "image2",
                Uri.parse("content://media/external/file/135"),
                startMs = 1500L
        ) { transformable -> transformableList.add(transformable) }

        composer.addImage(
                "image3",
                Uri.parse("content://media/external/file/136")
        ) { transformable -> transformableList.add(transformable) }

        // sphere
        composer.addVideo(
                "video1",
                Uri.parse("content://media/external/file/3365")
        ) { transformable -> transformableList.add(transformable) }*/

        // filmm
        /*composer.addVideo(
                "video2",
                Uri.parse("content://media/external/file/4024")
        ) { transformable -> transformableList.add(transformable) }*/

        //  with audio
        /*composer.addVideo(
                "video4",
                Uri.parse("content://media/external/file/3371")
        ) { transformable -> transformableList.add(transformable) }*/

        // harlem shake
        /*composer.addVideo(
                "video5",
                Uri.parse("content://media/external/file/342")
        ) { transformable -> transformableList.add(transformable) }*/

        // audio video sync
        /*composer.addVideo(
                "video6",
                Uri.parse("content://media/external/file/3366")
        ) { transformable -> transformableList.add(transformable) }*/

        // rabbit
        /*composer.addVideo(
                "video7",
                Uri.parse("content://media/external/file/3372")
        ) { transformable -> transformableList.add(transformable) }*/

        a1_1.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_1_1.value, true) }
        a3_2.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_3_2.value, true) }
        a2_3.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_2_3.value, true) }
        a4_5.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_4_5.value, true) }
        a5_4.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_5_4.value, true) }
        a9_16.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_9_16.value, true) }
        a16_9.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_16_9.value, true) }
        a18_9.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_18_9.value, true) }
        a9_18.setOnClickListener { composer.setAspectRatio(AspectRatio.RATIO_9_18.value, true) }

        btn_pick.setOnClickListener { checkPermissionsAndGetMedia(MainActivity.PERMISSIONS) }

        btn_render.setOnClickListener {
            startTimeNsec = System.nanoTime()
            showProgressDialog()

            bakeProcess = composer.renderToVideoFile(
                    requireContext(),
                    File(requireContext().cacheDir, "result.mp4").absolutePath,
                    outputMinSidePx = 1080,
                    fps = 30,
                    verboseLogging = true,
                    progressListener = { progress: Float, completed: Boolean ->
                        requireActivity().runOnUiThread { handleProgress(progress, completed) }
                    }
            )
        }

        btn_render_service.setOnClickListener {
            showProgressDialog()
            registerProgressReceiver()

            useReceiver = true
            startTimeNsec = System.nanoTime()
            bakeProcess = composer.renderToVideoFileInSeparateProcess(
                    requireContext(),
                    File(requireContext().cacheDir, "result.mp4").absolutePath,
                    outputMinSidePx = 1080,
                    fps = 30,
                    verboseLogging = true
            )
        }

        bottomView.alsoOnLaid { bottomView ->
            val maxHeight = container.height / 2
            bottomSeek.progress = ((bottomView.height / maxHeight.toFloat()) * 100).toInt()
            glView.setViewportMargin(bottom = bottomView.height)

            bottomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    bottomView.layoutParams = bottomView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportMargin(bottom = viewHeight)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        topView.alsoOnLaid { topView ->
            val maxHeight = container.height / 2
            topSeek.progress = ((topView.height / maxHeight.toFloat()) * 100).toInt()
            glView.setViewportMargin(top = topView.height)

            topSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val viewHeight = (maxHeight * (progress / 100f)).toInt()
                    topView.layoutParams = topView.layoutParams.apply {
                        height = viewHeight
                    }

                    glView.setViewportMargin(top = viewHeight)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }

        glView.listenTouches(object : LayerTouchListener {
            override fun onLayerTap(transformable: Transformable): Boolean {
                transformableList.forEach {
                    val clicked = it.id == transformable.id
                    it.enableGesturesTransform = clicked
                    it.setBorder(if (clicked) 1f else 0f, Color.GREEN)

                    if (clicked) it.setLayerPosition(transformableList.lastIndex)
                }

                return true
            }

            override fun onViewportInsideTap(): Boolean {
                transformableList.forEach {
                    it.enableGesturesTransform = false
                    it.setBorder(0f, Color.GREEN)
                }

                return true
            }

            override fun onViewportOutsideTap(): Boolean {
                transformableList.forEach {
                    if (it.enableGesturesTransform) {
                        it.setBorder(1f, Color.BLUE)
                    }
                }

                return true
            }
        })
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            progressDialog = childFragmentManager.findFragmentByTag(TAG_PROGRESS_DIALOG) as? ProgressDialog
            useReceiver = savedInstanceState.getBoolean(KEY_USE_RECEIVER, false)
            startTimeNsec = savedInstanceState.getLong(KEY_START_TIME, 0L)
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()

        if (useReceiver) {
            registerProgressReceiver()
        }
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()

        unregisterProgressReceiver()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_USE_RECEIVER, useReceiver)
        outState.putLong(KEY_START_TIME, startTimeNsec)

        super.onSaveInstanceState(outState)
    }

    private fun registerProgressReceiver() {
        progressReceiver = BakeProgressReceiver { progress, completed -> handleProgress(progress, completed) }
        requireContext()
            .registerReceiver(progressReceiver, IntentFilter(BakeProgressReceiver.ACTION_PUBLISH_PROGRESS))
    }

    private fun unregisterProgressReceiver() {
        if (progressReceiver != null) {
            requireContext().unregisterReceiver(progressReceiver)
            progressReceiver = null
        }
    }

    private fun showProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = ProgressDialog()
        progressDialog?.show(childFragmentManager, TAG_PROGRESS_DIALOG)
    }

    private fun Long.toSeconds() = this / 1_000_000_000f

    private fun handleProgress(value: Float, completed: Boolean) {
        Log.d(TAG, "handleProgress::$value : $completed")
        progressDialog?.setProgress(value)

        if (completed) {
            Log.d(TAG, "handleProgress::completed for ${(System.nanoTime() - startTimeNsec).toSeconds()} seconds")
            bakeProcess = null
            progressDialog?.dismiss()
            progressDialog = null

            unregisterProgressReceiver()
        }
    }
}