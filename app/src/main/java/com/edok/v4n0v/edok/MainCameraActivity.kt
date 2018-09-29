package com.edok.v4n0v.edok


import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraMetadata.CONTROL_MODE_AUTO
import android.hardware.camera2.CaptureRequest.CONTROL_MODE
import android.hardware.camera2.CaptureRequest.JPEG_ORIENTATION
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main_camera.*
import java.io.File
import java.io.FileOutputStream
import java.util.*


class MainCameraActivity : BaseActivity(), TextureView.SurfaceTextureListener {
    companion object {
        val ORIENTATIONS = SparseIntArray()
        val REQUEST_CAMERA_PERMITION = 200
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

    private lateinit var file: File
    private var isFlashSupported = false
    private lateinit var mBackgroundHandler: Handler
    private lateinit var mBackgrounfThread: HandlerThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_camera)
        photoView.surfaceTextureListener = this
        btnShot.setOnClickListener {
            takePicture()
        }
    }


    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        openCamera()
    }

    private fun openCamera() {

    }

    private fun takePicture() {
        if (cameraDevice == null)
            return
        val cameraManager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristicts = cameraManager.getCameraCharacteristics(cameraDevice?.id!!)
            var jpegSizes: Array<Size>? = null

            jpegSizes = characteristicts[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.getOutputSizes(ImageFormat.JPEG)

            var width = 640
            var height = 480
            if (jpegSizes != null && jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }

            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outSurfaces = mutableListOf<Surface>()
            outSurfaces.add(reader.surface)
            outSurfaces.add(Surface(photoView.surfaceTexture))

            val captBuilder: CaptureRequest.Builder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)!!
            captBuilder.addTarget(reader.surface)
            captBuilder.set(CONTROL_MODE, CONTROL_MODE_AUTO)

            //Orientation
            val rotation = windowManager.defaultDisplay.rotation
            captBuilder.set(JPEG_ORIENTATION, ORIENTATIONS[rotation])

            file = File("${Environment.getExternalStorageDirectory()}/${UUID.randomUUID()}.jpg")

            val readerListener = ImageReader.OnImageAvailableListener {
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    val buffer = image.planes[0].buffer
                    var bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    save(bytes)
                } catch (e: Exception) {
                    saveLog(e)
                } finally {
                    image?.close()
                }
            }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            var capListener: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    runOnUiThread { toast("Saved in $file") }
                    createCameraPreview()
                }
            }

            cameraDevice?.createCaptureSession(outSurfaces, object : CameraCaptureSession.StateCallback(){
                override fun onConfigured(session: CameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captBuilder.build(), capListener, mBackgroundHandler)
                    } catch (e:CameraAccessException){
                        saveLog(e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    saveLog(Exception("cameraDevice.createCaptureSession onConfigureFailed"))
                }

            }, mBackgroundHandler)

        } catch (e: CameraAccessException) {
            saveLog(e)
        }
    }

    private fun save(bytes: ByteArray) {
        FileOutputStream(file).use {
            it.write(bytes)
        }
    }


    private fun createCameraPreview() {

    }

}
