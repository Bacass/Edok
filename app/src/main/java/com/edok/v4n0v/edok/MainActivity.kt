package com.edok.v4n0v.edok

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.view.View

import android.hardware.Camera
import android.hardware.Camera.Size

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainScreen : Activity(), SurfaceHolder.Callback, View.OnClickListener, Camera.PictureCallback, Camera.PreviewCallback, Camera.AutoFocusCallback {
    private var camera: Camera? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var preview: SurfaceView? = null
    private var shotBtn: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // если хотим, чтобы приложение постоянно имело портретную ориентацию
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // если хотим, чтобы приложение было полноэкранным
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // и без заголовка
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.activity_main)

        // наше SurfaceView имеет имя SurfaceView01
        preview = findViewById<View>(R.id.SurfaceView01) as SurfaceView

        surfaceHolder = preview!!.holder
        surfaceHolder!!.addCallback(this)
        surfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        // кнопка имеет имя Button01
        shotBtn = findViewById<View>(R.id.Button01) as Button
        shotBtn!!.text = "Shot"
        shotBtn!!.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        camera = Camera.open()
    }

    override fun onPause() {
        super.onPause()

        if (camera != null) {
            camera!!.setPreviewCallback(null)
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            camera!!.setPreviewDisplay(holder)
            camera!!.setPreviewCallback(this)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val previewSize = camera!!.parameters.previewSize
        val aspect = previewSize.width.toFloat() / previewSize.height

        val previewSurfaceWidth = preview!!.width
        val previewSurfaceHeight = preview!!.height

        val lp = preview!!.layoutParams

        // здесь корректируем размер отображаемого preview, чтобы не было искажений

        if (this.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // портретный вид
            camera!!.setDisplayOrientation(90)
            lp.height = previewSurfaceHeight
            lp.width = (previewSurfaceHeight / aspect).toInt()
        } else {
            // ландшафтный
            camera!!.setDisplayOrientation(0)
            lp.width = previewSurfaceWidth
            lp.height = (previewSurfaceWidth / aspect).toInt()
        }

        preview!!.layoutParams = lp
        camera!!.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    override fun onClick(v: View) {
        if (v === shotBtn) {
            // либо делаем снимок непосредственно здесь
            // 	либо включаем обработчик автофокуса

            //camera.takePicture(null, null, null, this);
            camera!!.autoFocus(this)
        }
    }

    override fun onPictureTaken(paramArrayOfByte: ByteArray, paramCamera: Camera) {
        // сохраняем полученные jpg в папке /sdcard/CameraExample/
        // имя файла - System.currentTimeMillis()

        try {
            val saveDir = File("/sdcard/CameraExample/")

            if (!saveDir.exists()) {
                saveDir.mkdirs()
            }

            val os = FileOutputStream(String.format("/sdcard/CameraExample/%d.jpg", System.currentTimeMillis()))
            os.write(paramArrayOfByte)
            os.close()
        } catch (e: Exception) {
        }

        // после того, как снимок сделан, показ превью отключается. необходимо включить его
        paramCamera.startPreview()
    }

    override fun onAutoFocus(paramBoolean: Boolean, paramCamera: Camera) {
        if (paramBoolean) {
            // если удалось сфокусироваться, делаем снимок
            paramCamera.takePicture(null, null, null, this)
        }
    }

    override fun onPreviewFrame(paramArrayOfByte: ByteArray, paramCamera: Camera) {
        // здесь можно обрабатывать изображение, показываемое в preview
    }
}