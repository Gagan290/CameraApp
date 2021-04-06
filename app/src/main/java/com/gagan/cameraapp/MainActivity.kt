package com.gagan.cameraapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    var imgCapture: ImageView? = null
    var file: File? = null
    var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        imgCapture = findViewById(R.id.imgCapture)

        imgCapture?.setOnClickListener {
            file = CameraUtils.getOutputMediaFile(
                this, Constants.MEDIA_TYPE_IMAGE, "ProfileName", Constants.IMAGE_EXTENSION
            )
            uri = CameraUtils.getOutputMediaFileUri(this, file)

            startActivityForResult(
                CameraUtils.getPickerImageChooserIntent(this, uri),
                Constants.CAMERA_CAPTURE_IMAGE_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if the result is capturing Image
        if (requestCode == Constants.CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {

                    var myBitmap: Bitmap? = CameraUtils.optimizeBitmap(
                        Constants.BITMAP_SAMPLE_SIZE,
                        file?.absolutePath
                    )

                    var bitmap: Bitmap? = null
                    myBitmap?.let {
                        myBitmap = CameraUtils.getResizedBitmap(it, 500)
                        try {
                            bitmap =
                                uri?.let { it1 -> CameraUtils.rotateImageIfRequired(this, it, it1) }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error ${e.message}")
                        }
                    }
                    bitmap?.let {
                        imgCapture?.setImageBitmap(it)
                    }
                }
                RESULT_CANCELED -> {
                    // user cancelled Image capture
                    Toast.makeText(this, "User cancelled image capture", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // failed to capture image
                    Toast.makeText(this, "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}