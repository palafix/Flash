@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package nl.arnhem.flash.contracts

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import ca.allanwang.kau.permissions.PERMISSION_WRITE_EXTERNAL_STORAGE
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.utils.setIcon
import ca.allanwang.kau.utils.string
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs


/**
 * Created by Allan Wang on 2017-07-04.
 **/
const val MEDIA_CHOOSER_RESULT = 67

interface FileChooserActivityContract {
    fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams)
}

interface FileChooserContract {
    var filePathCallback: ValueCallback<Array<Uri>?>?
    fun Activity.openMediaPicker(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams)
    fun Activity.launchFilePicker(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams)
    fun Activity.onActivityResultWeb(requestCode: Int, resultCode: Int, intent: Intent?): Boolean
}

class FileChooserDelegate : FileChooserContract {

    override var filePathCallback: ValueCallback<Array<Uri>?>? = null

    override fun Activity.launchFilePicker(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        openMediaPicker(filePathCallback, fileChooserParams)
        return
    }

    @SuppressLint("InflateParams")
    @Suppress("LABEL_NAME_CLASH")
    override fun Activity.openMediaPicker(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        kauRequestPermissions(PERMISSION_WRITE_EXTERNAL_STORAGE) onActivityResultWeb@{ granted, _ ->
            if (!granted) {
                filePathCallback.onReceiveValue(null)
                return@onActivityResultWeb
            }
            this@FileChooserDelegate.filePathCallback = filePathCallback
            val view = layoutInflater.inflate(R.layout.bottom_sheet, null)
            val content = view.findViewById(R.id.popup_window) as RelativeLayout
            val titelText = view.findViewById(R.id.txt) as TextView
            val imagePicker = view.findViewById(R.id.txt_imagepicker) as TextView
            val videoPicker = view.findViewById(R.id.txt_videopicker) as TextView
            val imageImage = view.findViewById(R.id.txt_image) as ImageView
            val videoImage = view.findViewById(R.id.txt_video) as ImageView
            val mBottomSheetDialog = Dialog(this, R.style.MaterialDialogSheet)

            content.setBackgroundColor(Prefs.bgColor)
            titelText.setTextColor(Prefs.textColor)
            videoPicker.setTextColor(Prefs.textColor)
            imagePicker.setTextColor(Prefs.textColor)
            imageImage.setIcon(GoogleMaterial.Icon.gmd_image, 16, Prefs.accentColor)
            videoImage.setIcon(GoogleMaterial.Icon.gmd_video_library, 16, Prefs.accentColor)

            mBottomSheetDialog.setContentView(view)
            mBottomSheetDialog.window.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            mBottomSheetDialog.window.setGravity(Gravity.BOTTOM)

            imagePicker.setOnClickListener {
                this@FileChooserDelegate.filePathCallback = filePathCallback
                val intent = Intent("android.intent.action.PICK")
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, string(R.string.pick_image)), MEDIA_CHOOSER_RESULT)
                mBottomSheetDialog.dismiss()
            }

            videoPicker.setOnClickListener {
                this@FileChooserDelegate.filePathCallback = filePathCallback
                val intent = Intent("android.intent.action.PICK")
                intent.type = "video/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, string(R.string.pick_image)), MEDIA_CHOOSER_RESULT)
                mBottomSheetDialog.dismiss()
            }
            mBottomSheetDialog.show()

            mBottomSheetDialog.setOnCancelListener onActivityResultWeb@{
                this@FileChooserDelegate.filePathCallback = filePathCallback
                mBottomSheetDialog.dismiss()
                filePathCallback.onReceiveValue(null)
                return@onActivityResultWeb
            }
        }
    }


    override fun Activity.onActivityResultWeb(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        L.d { "FileChooser On activity results web $requestCode" }
        if (requestCode != MEDIA_CHOOSER_RESULT) return false
        val data = intent?.data
        filePathCallback?.onReceiveValue(if (data != null) arrayOf(data) else null)
        filePathCallback = null
        return true
    }
}






