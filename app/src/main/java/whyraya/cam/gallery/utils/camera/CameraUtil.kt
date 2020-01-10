package whyraya.cam.gallery.utils.camera

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.hardware.camera2.CameraDevice
import android.view.Surface
import whyraya.cam.gallery.data.CameraInterface
import whyraya.cam.gallery.utils.Utils.getImagePath
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

@Suppress( "deprecation" )
class CameraUtil(
    private val activity: Activity,
    private val mListener: CameraInterface? = null
) {

    var mCamera: Camera? = null

    private val cameraFront = Camera.CameraInfo.CAMERA_FACING_FRONT

    private val cameraBack = Camera.CameraInfo.CAMERA_FACING_BACK

    /**
     * ID of the current [CameraDevice].
     */
    private var cameraId = cameraBack

    private val flashModes = arrayOf(
        Camera.Parameters.FLASH_MODE_AUTO,
        Camera.Parameters.FLASH_MODE_TORCH,
        Camera.Parameters.FLASH_MODE_OFF
    )

    private var displayOrientation = 0

    private var flashMode = 0

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(cameraId)
        } catch (e: Exception) { null }
    }

    val mPicture get() = Camera.PictureCallback { data, _ ->
        val pictureFile = File(getImagePath(activity))
        try {
            // Despite of the displayOrientation value change
            // The image taken from camera will always at 0 angle (Landscape left)
            // So we need to rotate the image to match the displayOrientation
            val angle = displayOrientation.toFloat()
            val matrix = android.graphics.Matrix()
            matrix.postRotate(if (angle == 90f && cameraId == cameraFront) 270f else angle)
            val options = BitmapFactory.Options()
            options.inSampleSize = 2

            BitmapFactory.decodeStream(data.inputStream(), null, options)?.let {
                val bitmap = Bitmap.createBitmap(
                    it, 0, 0, it.width, it.height, matrix, true
                )
                val out = FileOutputStream(pictureFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
            }
            mListener?.onCaptureCompleted(pictureFile.absolutePath)
        } catch (e: FileNotFoundException) {
            mListener?.onInfo("File not found: ${e.message}")
        } catch (e: IOException) {
            mListener?.onInfo("Error accessing file: ${e.message}")
        } catch (e: java.lang.Exception) {
            mListener?.onInfo("Error : ${e.message}")
        }
    }

    fun switchCamera() {
        cameraId = if (cameraId == cameraBack) cameraFront else cameraBack
        onPause()
    }

    fun setFlashMode(mode: Int) {
        flashMode = mode
        mCamera?.apply {
            try {
                val param = parameters
                if (parameters.supportedFlashModes.contains(flashModes[flashMode]))
                    param.flashMode = flashModes[flashMode]
                parameters = param
            } catch (e: Exception) {
                mListener?.onInfo("Error : ${e.message}")
            }
        }
    }

    fun cameraWithDisplayOrientation() {
        val camera = getCameraInstance()
        var flash = false
        camera?.apply {
            try {
                flash = parameters.supportedFlashModes.contains(flashModes[flashMode])
                val param = parameters
                if (flash)
                    param.flashMode = flashModes[flashMode]
                if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                    param.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                parameters = param
                val info = Camera.CameraInfo()
                Camera.getCameraInfo(cameraId, info)
                val rotation = activity.windowManager.defaultDisplay
                    .rotation
                val degrees = when (rotation) {
                    Surface.ROTATION_0 ->  0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 ->  180
                    Surface.ROTATION_270 ->  270
                    else -> 0
                }

                displayOrientation =
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                        (360 - ((info.orientation + degrees) % 360)) % 360 // compensate the mirror
                    else // back-facing
                        (info.orientation - degrees + 360) % 360
                setDisplayOrientation(displayOrientation)
            } catch (e: Exception) {
                mListener?.onInfo("Error : ${e.message}")
            }
        }
        mListener?.flashSupported(flash)
        mCamera = camera
    }

    fun takePicture() {
        mCamera?.takePicture(null, null, mPicture)
    }

    fun onResume() {
        cameraWithDisplayOrientation()
    }

    fun onPause() {
        mCamera?.stopPreview()
        mCamera?.setPreviewCallback(null)
        mCamera?.release()
        mCamera = null
    }
}