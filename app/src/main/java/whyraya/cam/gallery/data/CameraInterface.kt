package whyraya.cam.gallery.data

interface CameraInterface {

    fun onCaptureCompleted(path: String)

    fun flashSupported(support: Boolean)

    fun onInfo(message: String)
}