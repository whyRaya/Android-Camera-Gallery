package whyraya.cam.gallery.ui

import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.material.appbar.AppBarLayout
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import whyraya.cam.gallery.R
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.utils.PermissionHelper
import whyraya.cam.gallery.utils.camera.CameraPreview
import whyraya.cam.gallery.utils.camera.CameraUtil
import whyraya.cam.gallery.utils.camera2.AutoFitTextureView
import whyraya.cam.gallery.utils.camera2.Camera2Util

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CameraRepository(application)

    var permission: PermissionHelper? = null

    val imageData = MutableLiveData<ArrayList<ImageModel>>(ArrayList())

    val mPreview = MutableLiveData<CameraPreview>()

    private var camera: CameraUtil? = null

    private var camera2: Camera2Util? = null

    val flashSupported = MutableLiveData(false)

    private val flashMode = MutableLiveData(0)

    private val compositeDisposable = CompositeDisposable()

    private val collapseAlpha =  MutableLiveData(1.0f)

    val mCollapseAlpha: LiveData<Float> = collapseAlpha

    val imagePath = repository.getImagePath()

    val flashIcon: LiveData<Int> = Transformations.map(flashMode) {
        when(it) {
            0 -> R.drawable.ic_flash_auto
            1 -> R.drawable.ic_flash_on
            else -> R.drawable.ic_flash_off
        }
    }

    fun getImagesFromGallery(pageSize: Int, list: (List<ImageModel>) -> Unit) {
        compositeDisposable.add(Single.fromCallable {
            repository.fetchGalleryImages(pageSize)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { list(it) },
                { it.printStackTrace() }
            )
        )
    }

    fun onOffsetChanged(appBarLayout: AppBarLayout) {
        val offsetAlpha = appBarLayout.y / appBarLayout.totalScrollRange
        collapseAlpha.value = 1 - (offsetAlpha * -1)
    }

    fun openCamera(activity: Activity, texture: AutoFitTextureView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            openCamera2(activity, texture)
        else
            openCamera(activity)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera2(activity: Activity, texture: AutoFitTextureView) {
        if (camera2 == null) {
            camera2 = Camera2Util(activity, texture, flashMode.value?:0,
                object : Camera2Util.Listener {
                    override fun onCaptureCompleted(path: String) {
                        repository.postImagePath(path)
                    }

                    override fun flashSupported(support: Boolean) {
                        flashSupported.value = support
                    }

                    override fun onInfo(message: String) {
                        repository.showMessage(message)
                    }

                    override fun onConfigured() {

                    }
                })
        }
        camera2?.updateTextureView(texture)
    }

    private fun openCamera(activity: Activity) {
        if (camera == null) {
            camera = CameraUtil(activity,flashMode.value?:0, object : CameraUtil.Listener {
                override fun onCaptureCompleted(path: String) {
                    repository.postImagePath(path)
                }

                override fun flashSupported(support: Boolean) {
                    flashSupported.value = support
                }

                override fun onInfo(message: String) {
                    repository.showMessage(message)
                }
            })
        }
        camera?.let {it2 ->
            it2.cameraWithDisplayOrientation()
            it2.mCamera?.let { mPreview.value = CameraPreview(activity, it) }
        }
    }

    fun onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.onResume()
        else if (camera?.mCamera == null) {
            camera?.onResume()
            camera?.mCamera?.let { mPreview.value = CameraPreview(repository.app, it) }
        }
    }

    fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.onPause()
        else
            camera?.onPause()
    }

    fun capture() {
        if (mCollapseAlpha.value?:0 == 1f) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                camera2?.lockFocus()
            else
                camera?.takePicture()
        }
    }

    fun setFlashMode() {
        if (flashSupported.value == true) {
            val value = ((flashMode.value ?: 0) + 1).let { if (it == 3) 0 else it }
            flashMode.value = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                camera2?.setFlashMode(value)
            else
                camera?.setFlashMode(value)
        }
    }

    fun switchCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.switchCamera()
        else {
            camera?.switchCamera()
            onResume()
        }
    }

    fun setImagePath(path: String) {
        repository.setImagePath(path)
    }

    override fun onCleared() {
        compositeDisposable.clear()
    }
}