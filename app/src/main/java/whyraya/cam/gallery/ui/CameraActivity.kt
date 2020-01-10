package whyraya.cam.gallery.ui

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import whyraya.cam.gallery.R
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.databinding.CameraBinding
import whyraya.cam.gallery.ui.adapter.GalleryAdapter
import whyraya.cam.gallery.utils.GridDecoration
import whyraya.cam.gallery.utils.PermissionHelper
import whyraya.cam.gallery.utils.Utils.dpToPx
import whyraya.cam.gallery.utils.Utils.getPathFromUri
import java.io.File
import java.util.*

class CameraActivity: AppCompatActivity(),
    GalleryAdapter.Listener, PermissionHelper.PermissionListener {

    private lateinit var binding: CameraBinding

    private lateinit var viewModel: CameraViewModel

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var adapter: GalleryAdapter

    // To detect scroll position from nestedScrollView
    private var vPosition = 0

    // To detect touch status, related to appBar expanded/collapse status
    private var touchDown = false

    // To refresh camera onStart or after orientation change
    private var reInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.camera)
        binding.lifecycleOwner = this

        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.permission = PermissionHelper(this)
        viewModel.permission?.setPermissionListener(this)
        // Clear every unnecessary data, in case of onCreate after orientation change
        viewModel.clear()

        //Init the layout for horizontal(on camera) and vertical (on nestedScrollView)
        layoutManager = GridLayoutManager(
            this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.itemImageList.itemAnimator = DefaultItemAnimator()
        binding.itemImageList.layoutManager = layoutManager
        binding.itemImageList.isNestedScrollingEnabled = false

        binding.galleryList.addItemDecoration(GridDecoration(3, dpToPx(this), true))
        binding.galleryList.itemAnimator = DefaultItemAnimator()
        binding.galleryList.layoutManager = GridLayoutManager(this, 3)
        binding.galleryList.isNestedScrollingEnabled = false

        // Set imageData from viewModel as the source data for Adapter
        // And since this variable comes from viewModel, it will survive the orientation change
        // So it will save your old data before Activity is recreated
        (viewModel.imageData.value?: LinkedList()).let {
            adapter = GalleryAdapter(it, this)
            binding.itemImageList.adapter = adapter
            binding.galleryList.adapter = adapter
        }

        // Observer for fetchGalleryImages data result on repository
        // It will update the viewModel.imageData then the adapter
        // And instead notifyDataSetChanged(), use notifyItemRangeInserted for better performance
        // Since we only update/add 25 data for each fetch
        viewModel.loadedImage.observe(this, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    viewModel.imageData.value?.addAll(it)
                    adapter.notifyItemRangeInserted(adapter.itemCount - it.size, it.size)
                }
            }
        })

        // Triggered when a user take a camera picture
        // It will insert the result/picture on the first list of Gallery
        // And show the info about its saved location
        viewModel.imagePath.observe(this, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    viewModel.imageData.value?.add(0, ImageModel(File(it).toUri()))
                    adapter.notifyItemRangeInserted(0, 1)
                    showInfo("Located at $it")
                }
            }
        })

        // Triggered when the camera not working, another error, etc
        viewModel.message.observe(this, Observer {
            it?.let { if (it.isNotEmpty()) showInfo(it) }
        })

        binding.appbar.setExpanded(true)
        trackMovement()
    }

    /**
     * Detect the motion or gesture from user and app
     * When they scroll, fling, touch up, etc
     * **/
    @SuppressLint("ClickableViewAccessibility")
    private fun trackMovement() {

        binding.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener {
                appBarLayout, _ ->
            viewModel.onOffsetChanged(appBarLayout)
        })

        // If the adapter reach the end of the item, fetch another 25 image
        binding.itemImageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                viewModel.loadPictures(
                    adapter.itemCount >= viewModel.loadImage &&
                            layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1
                )
            }
        })

        binding.nested.setOnScrollChangeListener {
                v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
            vPosition = scrollY
            viewModel.loadPictures(
                vPosition > 0 &&
                        vPosition == (v.getChildAt(0).measuredHeight) - v.measuredHeight
            )
        }

        // The main usage of this variable, is to change the alpha from camera button
        // e.g shutter, flash, and camera switcher
        // But there's a case when we scroll(fling) the nestedScrollView,
        // its stopped at half expand/collapse appBar
        // So we use this variable to also detect the motion fling
        viewModel.mCollapseAlpha.observe(this, Observer {
            it?.let {
                // appBar layout not fully expand/collapse & user just fling
                // set full expand (show full camera view)
                if (it > 0 && touchDown) {
                    touchDown = false
                    binding.nested.scrollTo(0, 0)
                    binding.appbar.setExpanded(true)
                }
            }
        })

        // Detect touch on camera,
        // If user touch up and the appBarLayout/camera is expand <= 75%. set full collapse
        // else back to full expand
        binding.coordinator.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                binding.appbar.setExpanded(viewModel.mCollapseAlpha.value?:0f >= 0.75f)
                true
            }
            else false
        }

        // Detect touch on nestedScrollView or the gallery below camera
        // If user touch up and the appBarLayout/camera is expand >= 10%. set full expand
        binding.nested.setOnTouchListener { _, event ->
            touchDown = false
            if (vPosition == 0 && event?.action == MotionEvent.ACTION_UP) {
                binding.appbar.setExpanded(viewModel.mCollapseAlpha.value?:0f >= 0.1f)
                true
            }
            else {
                if (event?.action == MotionEvent.ACTION_UP)
                    touchDown = true
                false
            }
        }
    }

    /**
     * Because this feature needs be active and without having a need to press a button
     * Always check its permission status at onResume,
     * */
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val listPermissions = ArrayList<String>()
            listPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            listPermissions.add(Manifest.permission.CAMERA)
            viewModel.permission?.checkAndRequestPermissions(listPermissions)
        } else
            onPermissionCheckDone()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onChooseImg(uri: Uri) {
        showInfo("Located at ${getPathFromUri(this, uri)?:""}")
    }

    private fun showInfo(info: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content),
            info,
            Snackbar.LENGTH_INDEFINITE
        )
        snackBar.setAction("OK") { snackBar.dismiss() }
        snackBar.show()
    }

    /**
     * After all permission granted 'Camera & Storage'
     * */
    override fun onPermissionCheckDone() {
        if (reInit)
            viewModel.openCamera(this, binding.texture)

        reInit = false
        viewModel.onResume()
        // load picture if the imageData or adapter still empty
        viewModel.loadPictures((viewModel.imageData.value?: LinkedList()).size == 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.permission?.onRequestCallBack(requestCode, permissions, grantResults)
    }
}