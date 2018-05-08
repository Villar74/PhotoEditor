package com.villar.photoeditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.widget.*
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.ImageSize
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var bitmap1: Bitmap
    private lateinit var imageView: ImageButton
    private lateinit var targetSize: ImageSize
    private lateinit var imageUri: Uri
    private val reqWidth = 400
    private var imageLoader: ImageLoader? = null
    private var options: DisplayImageOptions? = null
    private var progressBarStatus = 0
    var dummy: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1)
        }
        val config = ImageLoaderConfiguration.Builder(this)
                .diskCacheExtraOptions(480, 800, null)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(100)
                .build()
        ImageLoader.getInstance().init(config)
        options = DisplayImageOptions.Builder()
                .delayBeforeLoading(1000)
                .build()
        imageLoader = ImageLoader.getInstance()
        val imageButton = findViewById<ImageButton>(R.id.imageButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        imageView = findViewById(R.id.imageView)
        imageButton!!.setOnClickListener {
            showPictureDialog()
        }
        imageView.setOnClickListener {
            showSaveDialog()
        }

        imageButton.minimumHeight = 400
        imageButton.minimumWidth = 400
        val outValue = TypedValue()

        applicationContext.theme.resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true)
        imageButton.setBackgroundResource(outValue.resourceId)
        imageView.setBackgroundResource(outValue.resourceId)

        Log.d("test", "onCreate")

        val rot = findViewById<Button>(R.id.rotate_button)
        rot!!.setOnClickListener {
            processImage(progressBar, "rotate")
        }
        val inv = findViewById<Button>(R.id.invert_button)
        inv!!.setOnClickListener {
            processImage(progressBar, "invert")

        }
        val mir = findViewById<Button>(R.id.mirror_button)
        mir!!.setOnClickListener {
            processImage(progressBar, "mirror")
        }
    }

    private fun processImage(progressBar: ProgressBar, action: String) {
        if (::bitmap1.isInitialized) {
            dummy = 0
            progressBarStatus = 0
            progressBar.progress = progressBarStatus
            Thread(Runnable {
                // performing some dummy operation
                // tracking progress
                when (action) {
                    "rotate" -> bitmap1 = bitmap1.rotate(90)
                    "invert" -> bitmap1 = invertColors(bitmap1)
                    "mirror" -> bitmap1 = mirrorBM(bitmap1)
                }

                val random = Random()

                val step = random.nextInt(10 - 2) + 2
                // dummy thread mimicking some operation whose progress can be tracked
                // dummy thread mimicking some operation whose progress can be tracked
                while (progressBarStatus < 100) {
                    // performing some dummy operation
                    try {
                        dummy += step
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    // tracking progress
                    progressBarStatus = dummy

                    // Updating the progress bar
                    progressBar.progress = progressBarStatus

                }
                this@MainActivity.runOnUiThread({
                    imageView.setImageBitmap(bitmap1)
                })
            }).start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        bitmap1 = createBitmap(100, 100, conf)
        imageView.setImageBitmap(bitmap1)
        if (requestCode == REQUEST_SELECT_IMAGE_IN_ALBUM) {
            if (data != null) {
                try {
                    imageUri = data.data
                    getTargetSize()
                    bitmap1 = imageLoader!!.loadImageSync(imageUri.toString(), targetSize, options)
                    imageLoader!!.displayImage(imageUri.toString(), imageButton, targetSize)

                } catch (e: IOException) {
                    e.printStackTrace()
                    toast("Failed!")
                }
            }

        } else if (requestCode == REQUEST_TAKE_PHOTO) {
            if (data !== null) {
                bitmap1 = data!!.extras!!.get("data") as Bitmap
                imageButton!!.setImageBitmap(bitmap1)
                toast("Image Saved!")
            }
        }
    }

    private fun getTargetSize() {
        val imageStream = contentResolver.openInputStream(imageUri)
        val selectedImage = BitmapFactory.decodeStream(imageStream)
        val scale = selectedImage.width / selectedImage.height
        val height = Math.round(reqWidth.toFloat() / scale)
        targetSize = ImageSize(reqWidth, height)
    }

    fun downloadImage(uri: String): Bitmap {
        val image = imageLoader!!.loadImageSync(uri, ImageSize(reqWidth, 200), options)
        if (image == null) {
            val conf = Bitmap.Config.ARGB_8888 // see other conf types
            return createBitmap(100, 100, conf)
        } else
            return image
    }

    private fun invertColors(bmp: Bitmap): Bitmap {
        val invertMap: FloatArray = floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        val colorMatrix = ColorMatrix()
        //colorMatrix.setSaturation(0f)

        val finalCM = ColorMatrix(colorMatrix)
        val invertCM = ColorMatrix(invertMap)
        finalCM.postConcat(invertCM)

        val filter = ColorMatrixColorFilter(finalCM)
        val bm = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        val paint = Paint()
        paint.colorFilter = filter
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return bm
    }

    private fun mirrorBM(bm: Bitmap): Bitmap {
        val matrixMirror = Matrix()
        matrixMirror.preScale(-1.0f, 1.0f)
        return Bitmap.createBitmap(
                bm,
                0,
                0,
                bm.getWidth(),
                bm.getHeight(),
                matrixMirror,
                false)
    }

    fun Bitmap.rotate(degree: Int): Bitmap {
        // Initialize a new matrix
        val matrix = Matrix()

        // Rotate the bitmap
        matrix.postRotate(degree.toFloat())

        // Resize the bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(
                this,
                width,
                height,
                true
        )

        // Create and return the rotated bitmap
        return Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrix,
                true
        )
    }

    private fun saveImage(finalBitmap: Bitmap, image_name: String) {
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File(root)
        myDir.mkdirs()
        val fname = "/Image-$image_name.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        Log.i("LOAD", root + fname)
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Toast.makeText(applicationContext,
                    "Сохранено", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun takePhoto() {
        val intent1 = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent1.resolveActivity(packageManager) != null) {
            startActivityForResult(intent1, REQUEST_TAKE_PHOTO)
        }
    }

    private fun selectImageInAlbum() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra("crop", "false")
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM)
        }
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val view = TableLayout(this)
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        val linearLayout = LinearLayout(this)
        linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
                50,
                20)
        input.layoutParams = lp
        input.setHint(R.string.download)
        //pictureDialog.setView(input)
        view.addView(input)
        val button = Button(this)
        val lp1 = LinearLayout.LayoutParams(
                50,
                20)
        button.layoutParams = lp1
        button.setText(R.string.download_button)
        button.setOnClickListener {
            if (input.text.isNotEmpty()) {
                bitmap1 = downloadImage(input.text.toString())
            }
        }
        view.addView(button)
        pictureDialog.setView(view)
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> selectImageInAlbum()
                1 -> takePhoto()
            }
        }
        pictureDialog.show()
    }

    private fun showSaveDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Сохранить")
        alertDialog.setMessage("Введите название")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        alertDialog.setView(input)
        alertDialog.setPositiveButton("Сохранить", { _, _ ->
            saveImage(bitmap1, input.text.toString())
        })

        alertDialog.setNegativeButton("Отмена", { _, _ ->
            Toast.makeText(applicationContext,
                    "Отмена", Toast.LENGTH_SHORT).show()
        })

        alertDialog.show()
    }

    companion object {
        private const val REQUEST_TAKE_PHOTO = 0
        private const val REQUEST_SELECT_IMAGE_IN_ALBUM = 1
    }
}
