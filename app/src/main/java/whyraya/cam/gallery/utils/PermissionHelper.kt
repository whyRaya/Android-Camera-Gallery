package whyraya.cam.gallery.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import whyraya.cam.gallery.R

/**
 * Created by raya on 5/4/17.
 */

class PermissionHelper(private val activity: Activity) {

    private val listPermissionsNeeded = ArrayList<String>()
    private var listPermissions: ArrayList<String> = ArrayList()
    private var listener: PermissionListener? = null

    private val REQUEST_PERMISSION = 789

    fun setPermissionListener(listener: PermissionListener) {
        this.listener = listener
    }

    fun checkAndRequestPermissions(permissions: String) {
        listPermissions.clear()
        listPermissions.add(permissions)
        checkAndRequestPermissions()
    }

    fun checkAndRequestPermissions(listPermissions: ArrayList<String>) {
        this.listPermissions = listPermissions
        checkAndRequestPermissions()
    }

    /**
     * Call this to check permission.
     * Will looping for check permission until main_account Approved it
     */
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            listPermissionsNeeded.clear()
            for (permission in listPermissions) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    listPermissionsNeeded.add(permission)
            }
            if (listPermissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    listPermissionsNeeded.toTypedArray(),
                    REQUEST_PERMISSION
                )
                return
            }
        }
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        listener!!.onPermissionCheckDone()
    }

    /**
     * Handling permission callback after you click deny or ok
     */
    fun onRequestCallBack(
        RequestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {//2. call this inside onRequestPermissionsResult
        if (RequestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty()) {
                var granted = true
                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        granted = false //false, if one permission not granted
                }

                // all permission granted
                if (granted)
                    checkAndRequestPermissions()
                else {
                    // Some permissions are not granted ask again. Ask again explaining the usage of permission.
                    var neverAsk = true
                    for (permission in listPermissions) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                permission
                            )
                        )
                            neverAsk = false
                    }
                    if (!neverAsk) {
                        showDialogOK { _, which ->
                            if (which == DialogInterface.BUTTON_POSITIVE)
                                checkAndRequestPermissions()
                        }
                    } else { //permission is denied (and never ask again is  checked)
                        Toast.makeText(
                            activity,
                            activity.resources.getString(R.string.info_never_ask_permission),
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.data = Uri.fromParts("package", activity.packageName, null)
                        activity.startActivityForResult(intent, REQUEST_PERMISSION)
                    }
                }
            }
        }
    }

    private fun showDialogOK(okListener: (Any, Any) -> Unit) {
        AlertDialog.Builder(activity)
            .setMessage(activity.resources.getString(R.string.info_need_permission))
            .setPositiveButton("OK", okListener)
            .create()
            .show()
    }

    interface PermissionListener {
        fun onPermissionCheckDone()
    }

}