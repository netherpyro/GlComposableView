package com.netherpyro.glcv.ui

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.netherpyro.glcv.App
import com.netherpyro.glcv.AspectRatio
import com.netherpyro.glcv.R
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.addDivider
import com.netherpyro.glcv.alsoOnLaid
import com.netherpyro.glcv.attrValue
import com.netherpyro.glcv.baker.BakeProgressReceiver
import com.netherpyro.glcv.baker.Cancellable
import com.netherpyro.glcv.baker.EncodeTarget
import com.netherpyro.glcv.baker.renderToVideoFile
import com.netherpyro.glcv.baker.renderToVideoFileInSeparateProcess
import com.netherpyro.glcv.compose.Composer
import com.netherpyro.glcv.compose.Controllable
import com.netherpyro.glcv.compose.playback.IPlaybackController
import com.netherpyro.glcv.compose.playback.PlaybackEventListener
import com.netherpyro.glcv.getActionBarSize
import com.netherpyro.glcv.playVideo
import com.netherpyro.glcv.saveToGallery
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
        private const val TAG_RENDER_DIALOG = "TAG_RENDER_DIALOG"

        private const val KEY_USE_RECEIVER = "KEY_USE_RECEIVER"
        private const val KEY_START_TIME = "KEY_START_TIME"

        // should be stored at lifecycle aware environment
        private val composer = Composer(App.instance).apply {
            setViewportColor(Color.DKGRAY)
            setBaseColor(Color.WHITE)
        }

        private var bakeProcess: Cancellable? = null
        private val playbackController: IPlaybackController = composer.getPlaybackController()
    }

    private val outputFile by lazy { File(requireContext().cacheDir, "result.mp4") }
    private val transformableList = mutableListOf<Transformable>()
    private val controllableList = mutableListOf<Controllable>()

    private val getMedia = registerForActivityResult(GetMultipleMedia()) { uriList ->
        uriList?.forEach { uri ->
            composer.addMedia(uri.toString(), uri) { transformable -> transformableList.add(transformable) }
                .also { controllable -> controllable?.let { controllableList.add(it) } }

            invalidateRenderBtn()
            invalidatePlayPauseBtn()
        }
    }

    private val checkPermissionsAndGetMedia = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.values.fold(false) { acc, granted -> acc or granted }
        if (granted) getMedia.launch("image/* video/*")
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

    private val aspectRatioAdapter: AspectRatioAdapter = AspectRatioAdapter(
            AspectRatio.values()
                .map { ar ->
                    AspectRatioItem(ar, ar.title, ar.value in composer.aspectRatio - 0.01f..composer.aspectRatio + 0.01f)
                }
    ) { selectedAspectRatio -> composer.setAspectRatio(selectedAspectRatio.value, true) }

    private val playbackEventListener = object : PlaybackEventListener {
        override fun onProgress(positionMs: Long) {
            // todo show progress view
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            invalidatePlayPauseBtn()
        }

        override fun onPlaybackEnded() {
            playbackController.seek(0L)
            playbackController.play()
        }
    }

    private var progressReceiver: BakeProgressReceiver? = null
    private var progressDialog: ProgressDialog? = null
    private var useReceiver = false
    private var startTimeNsec: Long = 0
    private var primaryColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.setFragmentResultListener(ProgressDialog.CODE_REQUEST_CANCEL, this) { _, _ ->
            bakeProcess?.cancel()
            bakeProcess = null
            progressDialog = null
        }

        childFragmentManager.setFragmentResultListener(RenderDialog.CODE_REQUEST_RENDER, this) { _, bundle ->
            startRender(bundle)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.f_compose, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        primaryColor = requireContext().attrValue(R.attr.colorPrimary)
        requireActivity().window.statusBarColor = primaryColor

        glView.enableGestures = true

        rv_aspect_ratio.alsoOnLaid {
            glView.setViewportMargin(
                    top = requireContext().getActionBarSize() + it.height + it.marginTop + it.marginBottom
            )
        }
        pane_control.alsoOnLaid { glView.setViewportMargin(bottom = it.height + it.marginBottom + it.marginTop) }

        composer.setBaseColor(requireContext().attrValue(R.attr.colorSurface))
        composer.enableOnClickLayerIteration(true)

        val addedControllables = composer.setGlView(glView) { transformable -> transformableList.add(transformable) }
        controllableList.clear()
        controllableList.addAll(addedControllables)

        invalidateRenderBtn()
        invalidatePlayPauseBtn()

        with(rv_aspect_ratio) {
            addDivider()
            adapter = aspectRatioAdapter
        }

        fab_pick.setOnClickListener { checkPermissionsAndGetMedia.launch(MainActivity.PERMISSIONS) }
        btn_render.setOnClickListener {
            RenderDialog().show(childFragmentManager, TAG_RENDER_DIALOG)
            playbackController.pause()
        }
        btn_play_pause.setOnClickListener { playbackController.togglePlay() }
        btn_replay.setOnClickListener { playbackController.seek(0) }

        glView.listenTouches(object : LayerTouchListener {
            override fun onLayerTap(transformable: Transformable): Boolean {
                transformableList.forEach {
                    val clicked = it.id == transformable.id
                    it.enableGesturesTransform = clicked
                    it.setBorder(if (clicked) 1f else 0f, primaryColor)
                    it.setOpacity(if (clicked) 1f else 0.3f)

                    if (clicked) it.setLayerPosition(transformableList.lastIndex)
                }

                return true
            }

            override fun onViewportInsideTap(): Boolean {
                transformableList.forEach {
                    it.enableGesturesTransform = false
                    it.setBorder(0f, primaryColor)
                    it.setOpacity(1f)
                }

                return true
            }

            override fun onViewportOutsideTap(): Boolean {
                clearSelection()

                return true
            }
        })
    }

    private fun clearSelection() {
        transformableList.forEach {
            if (it.enableGesturesTransform) {
                it.setBorder(0f, primaryColor)
            }

            it.setOpacity(1f)
        }
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

        playbackController.setPlaybackEventsListener(playbackEventListener)
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
        playbackController.pause()
        playbackController.setPlaybackEventsListener(null)

        unregisterProgressReceiver()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_USE_RECEIVER, useReceiver)
        outState.putLong(KEY_START_TIME, startTimeNsec)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        rv_aspect_ratio.adapter = null
        super.onDestroyView()
    }

    private fun invalidateRenderBtn() {
        btn_render.isEnabled = controllableList.isNotEmpty()
    }

    private fun invalidatePlayPauseBtn() {
        btn_play_pause.isEnabled = controllableList.isNotEmpty()
        btn_replay.isEnabled = controllableList.isNotEmpty()
        btn_play_pause.setImageResource(
                if (playbackController.isPlaying()) R.drawable.ic_pause_24 else R.drawable.ic_play_24)
    }

    private fun registerProgressReceiver() {
        progressReceiver = BakeProgressReceiver { encodeTarget, progress, completed -> handleProgress(encodeTarget, progress, completed) }
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

    private fun startRender(options: Bundle) {
        clearSelection()
        showProgressDialog()
        startTimeNsec = System.nanoTime()
        val outputPath = outputFile.absolutePath
        val fps = options.getInt(RenderDialog.KEY_FPS)
        val outputMinSidePx = options.getInt(RenderDialog.KEY_SIDE_SIZE)

        Log.d(TAG,
                "fps=$fps, res=$outputMinSidePx, separate service=${options.getBoolean(RenderDialog.KEY_USE_SERVICE)}")

        if (options.getBoolean(RenderDialog.KEY_USE_SERVICE)) {
            registerProgressReceiver()

            useReceiver = true
            bakeProcess = composer.renderToVideoFileInSeparateProcess(
                    requireContext(),
                    outputPath,
                    outputMinSidePx = outputMinSidePx,
                    fps = fps,
                    verboseLogging = true
            )
        } else {
            bakeProcess = composer.renderToVideoFile(
                    requireContext(),
                    outputPath,
                    outputMinSidePx = outputMinSidePx,
                    fps = fps,
                    verboseLogging = true,
                    progressListener = { encodeTarget: EncodeTarget, progress: Float, completed: Boolean ->
                        requireActivity().runOnUiThread { handleProgress(encodeTarget, progress, completed) }
                    }
            )
        }
    }

    private fun handleProgress(encodeTarget: EncodeTarget, value: Float, completed: Boolean) {
        Log.d(TAG, "handleProgress::$encodeTarget @ $value : $completed")
        progressDialog?.setProgress(encodeTarget, value)

        if (completed) {
            Log.d(TAG, "handleProgress::completed for ${(System.nanoTime() - startTimeNsec).toSeconds()} seconds")
            bakeProcess = null
            progressDialog?.dismiss()
            progressDialog = null

            unregisterProgressReceiver()

            // store and play baked video
            with(requireContext()) {
                saveToGallery(outputFile)?.let {
                    Handler(Looper.getMainLooper()).postDelayed({ playVideo(it) }, 1000L)
                }
            }
        }
    }

    private fun Long.toSeconds() = this / 1_000_000_000f
}