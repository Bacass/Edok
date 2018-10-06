package com.edok.v4n0v.edok


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraMetadata.CONTROL_MODE_AUTO
import android.hardware.camera2.CaptureRequest.CONTROL_MODE
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import kotlinx.android.synthetic.main.activity_main_camera.*
import java.io.*
import java.util.*
import android.graphics.Bitmap




class MainCameraActivity : BaseActivity() {
    companion object {
        val ORIENTATIONS = SparseIntArray()
        const val REQUEST_CAMERA_PERMISSION = 200
        const val THREAD_NAME = "Camera Background"
        val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    private lateinit var cameraId: String
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var imgDimen: Size
    private lateinit var imgReader: ImageReader

    private val photoList = arrayListOf<Bitmap>()

    private lateinit var file: File
    private var isFlashSupported = false
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            makeLog("onSurfaceTextureSizeChanged")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            makeLog("onSurfaceTextureUpdated")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            makeLog("onSurfaceTextureAvailable")
            openCamera()
        }

    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> if (grantResults[0] != PERMISSION_GRANTED) {
                toast(resources.getString(R.string.camera_permission_err))
                makeLog(Exception("REQUEST_CAMERA_PERMISSION failed"))
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (photoTextureView.isAvailable)
            openCamera()
        else
            photoTextureView.surfaceTextureListener = textureListener

    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread(THREAD_NAME)
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_camera)
        photoTextureView.surfaceTextureListener = textureListener
        btnShot.setOnClickListener {
            takePicture()
        }
    }

    override fun onPause() {
        stopBackgroundThread()
        super.onPause()
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            makeLog(e)
        }
    }


    private fun takePicture() {
        if (cameraDevice == null)
            return
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice?.id)
            var jpegSizes: Array<Size>? = null
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG)

            //Capture image with custom size
            var width = 640
            var height = 480
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurface = ArrayList<Surface>(2)
            outputSurface.add(reader.surface)
            outputSurface.add(Surface(photoTextureView.surfaceTexture))

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder?.addTarget(reader.surface)
            captureBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            //Check orientation base on device
            val rotation = windowManager.defaultDisplay.rotation
            captureBuilder?.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

            file = File(Environment.getExternalStorageDirectory().toString() + "/" + UUID.randomUUID().toString() + ".jpg")
            val readerListener = object : ImageReader.OnImageAvailableListener {
                override fun onImageAvailable(imageReader: ImageReader) {
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()


                        val buffer = image!!.planes[0].buffer
                        val bytes = ByteArray(buffer.capacity())
                        buffer.get(bytes)

                        photoList.add(formatBitmapForPreview(bytes))
                        bytes.save()
                        updateView()

                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        run { image?.close() }
                    }
                }


            }

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    toast("Saved $file")
                    createCameraPreview()
                }
            }

            cameraDevice?.createCaptureSession(outputSurface, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder?.build(), captureListener, mBackgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }

                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {

                }
            }, mBackgroundHandler)


        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun formatBitmapForPreview(bytes: ByteArray):Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        if (bitmap.width >bitmap.height){
            val matrix = Matrix()
            matrix.postRotate(90F)
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            makeLog("Width is ${rotated.width}")

            return Bitmap.createBitmap( rotated,
                    0,
                    (rotated.height*0.8).toInt(),
                    rotated.width,
                    (rotated.height*0.2).toInt())
        }

        return bitmap




}

private fun updateView() {
    runOnUiThread {
        if (photoList.size > 0) {

            if (imgLastPhoto.visibility == View.GONE)
                imgLastPhoto.visibility = View.VISIBLE
            imgLastPhoto.setImageBitmap(photoList[photoList.size - 1])
        }
        //   imgLastPhoto.setImageDrawable(photoList[photoList.size])
    }

}


private fun ByteArray.save() {
    FileOutputStream(file).use {
        it.write(this)
    }
}


private fun createCameraPreview() {
    try {
        val texture = photoTextureView.surfaceTexture
        texture?.setDefaultBufferSize(imgDimen.width, imgDimen.height)
        val surface = Surface(texture)
        captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)!!
        captureRequestBuilder.addTarget(surface)
        cameraDevice?.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                makeLog("createCameraPreview, onConfigureFailed")
            }

            override fun onConfigured(session: CameraCaptureSession) {
                if (cameraDevice == null)
                    return

                cameraCaptureSession = session
                updatePreview()
            }

        }, mBackgroundHandler)
    } catch (e: CameraAccessException) {
        makeLog(e)
    }
}

private fun updatePreview() {
    if (cameraDevice == null)
        makeLog(Exception("update preview failed, cameraDevice is null "))
    captureRequestBuilder.set(CONTROL_MODE, CONTROL_MODE_AUTO)
    try {
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler)
    } catch (e: CameraAccessException) {
        makeLog(e)
    }
}

private fun openCamera() {
    val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        if (cameraDevice != null)
    try {
        cameraId = manager.cameraIdList[0]

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        imgDimen = map.getOutputSizes(SurfaceTexture::class.java)[0]
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CAMERA_PERMISSION)
            return
        }
        manager.openCamera(cameraId, stateCallback, null)
    } catch (e: CameraAccessException) {
        makeLog(e)
    }

}
}
