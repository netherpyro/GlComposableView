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
import android.widget.LinearLayout
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.netherpyro.glcv.AspectRatio
import com.netherpyro.glcv.R
import com.netherpyro.glcv.Transformable
import com.netherpyro.glcv.attrValue
import com.netherpyro.glcv.baker.BakeProgressReceiver
import com.netherpyro.glcv.baker.Cancellable
import com.netherpyro.glcv.baker.renderToVideoFile
import com.netherpyro.glcv.compose.Composer
import com.netherpyro.glcv.compose.Controllable
import com.netherpyro.glcv.getActionBarSize
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

        // should be stored at lifecycle aware environment
        private val composer = Composer().apply {
            setViewportColor(Color.BLACK)
            setBaseColor(Color.WHITE)
        }

        private var bakeProcess: Cancellable? = null
    }

    private val transformableList = mutableListOf<Transformable>()
    private val controllableList = mutableListOf<Controllable>()

    private val getMedia = registerForActivityResult(GetMultipleMedia()) { uriList ->
        uriList?.forEach { uri ->
            composer.addMedia(uri.toString(), uri) { transformable -> transformableList.add(transformable) }
                .also { controllable -> controllable?.let { controllableList.add(it) } }

            invalidateRenderBtn()
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

    private val aspectRatioAdapter: AspectRatioAdapter = AspectRatioAdapter(
            AspectRatio.values()
                .map { ar ->
                    AspectRatioItem(ar, ar.title, ar.value in composer.aspectRatio - 0.1f..composer.aspectRatio + 0.1f)
                }
    ) { selectedAspectRatio -> composer.setAspectRatio(selectedAspectRatio.value, true) }

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
        glView.setViewportMargin(top = requireContext().getActionBarSize())

        composer.setBaseColor(requireContext().attrValue(R.attr.colorSurface))

        val addedControllables = composer.setGlView(glView) { transformable -> transformableList.add(transformable) }
        controllableList.clear()
        controllableList.addAll(addedControllables)

        invalidateRenderBtn()

        with(rv_aspect_ratio) {
            addDivider()
            adapter = aspectRatioAdapter
        }

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

        fab_pick.setOnClickListener { checkPermissionsAndGetMedia(MainActivity.PERMISSIONS) }

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

        /*btn_render_service.setOnClickListener {
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
        }*/

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

    override fun onDestroyView() {
        rv_aspect_ratio.adapter = null
        super.onDestroyView()
    }

    private fun invalidateRenderBtn() {
        btn_render.isEnabled = controllableList.isNotEmpty()
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

    private fun RecyclerView.addDivider() {
        val decoration =
                DividerItemDecoration(requireContext(), LinearLayout.HORIZONTAL)
                    .apply { setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.shape_divider)!!) }
        addItemDecoration(decoration)
    }
}