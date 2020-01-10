package whyraya.cam.gallery.ui

import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.material.appbar.AppBarLayout
import whyraya.cam.gallery.R
import whyraya.cam.gallery.data.CameraInterface
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.utils.PermissionHelper
import whyraya.cam.gallery.utils.camera.CameraUtil
import whyraya.cam.gallery.utils.camera2.AutoFitTextureView
import whyraya.cam.gallery.utils.camera2.Camera2Util
import java.util.*

class CameraViewModel(application: Application) :
    AndroidViewModel(application), CameraInterface {

    private val repository = CameraRepository(application)

    var permission: PermissionHelper? = null

    val loadImage = 25

    val imageData = MutableLiveData<LinkedList<ImageModel>>(LinkedList())

    val loadedImage = repository.getLoadedImage()

    val imagePath = repository.getImagePath()

    val message = repository.getMessage()

    val mPreview = repository.getCameraPreview()

    val flashSupported = repository.isSupportFlash()

    private val collapseAlpha =  MutableLiveData(1.0f)

    val mCollapseAlpha: LiveData<Float> = collapseAlpha

    val flashIcon: LiveData<Int> = Transformations.map(repository.getFlashMode()) {
        when(it) {
            0 -> R.drawable.ic_flash_auto
            1 -> R.drawable.ic_flash_on
            else -> R.drawable.ic_flash_off
        }
    }

    fun loadPictures(args: Boolean) {
        if (args)
            repository.fetchGalleryImages(loadImage)
    }

    fun onOffsetChanged(appBarLayout: AppBarLayout) {
        collapseAlpha.value = 1 - ((appBarLayout.y / appBarLayout.totalScrollRange) * -1)
    }

    /**
     * CameraAPI2 for LOLLIPOP API 21 and above
     * https://github.com/android/camera-samples/tree/master/Camera2BasicKotlin
     *
     * and old cameraAPI for API 20 and below
     * https://developer.android.com/guide/topics/media/camera.html
     * */
    fun openCamera(activity: Activity, texture: AutoFitTextureView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            repository.camera2 = Camera2Util(activity, texture, this)
//            if (repository.camera2 == null) {
//                repository.camera2 = Camera2Util(activity, texture, this)
//            }
//            repository.camera2?.updateTextureView(texture)
        }
        else {
            if (repository.camera == null)
                repository.camera = CameraUtil(activity, this)
        }
    }

    fun onResume() {
        repository.onResume()
    }

    fun onPause() {
        repository.onPause()
    }

    fun capture() {
        repository.capture(mCollapseAlpha.value?:0 == 1f)
    }

    fun setFlashMode() {
        repository.setFlashMode()
    }

    fun switchCamera() {
        repository.switchCamera()
    }

    fun clear() {
        repository.clear()
    }

    override fun onCaptureCompleted(path: String) {
        repository.postImagePath(path)
    }

    override fun flashSupported(support: Boolean) {
        repository.setSupportedFlash(support)
    }

    override fun onInfo(message: String) {
        repository.showMessage(message)
    }
}