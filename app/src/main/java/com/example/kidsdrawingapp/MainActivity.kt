package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null  // for color selecting
    private var customProgressDialog: Dialog? = null

    private val openGalleryLauncher: ActivityResultLauncher<Intent> = // Intent type launcher
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            /* So here we just get the result and we can now work with it.
             So this lambda, basically this part here register for activity result allows us to execute what we
             want to run in the case of the result that we're given and it gives us the result at the same time.*/
                result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                // will set image to image view(in our background)
                val imageBackGround: ImageView = findViewById(R.id.iv_background)
                imageBackGround.setImageURI(result.data?.data)

            }
        }


    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val perMissionName = it.key
                val isGranted = it.value

                if (perMissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (isGranted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permission granted now you can read the storage files.",
                            Toast.LENGTH_LONG
                        ).show()

                        // using intent to open gallery
                        val pickIntent =
                            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        // start the intent+ set image to background
                        openGalleryLauncher.launch(pickIntent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


            }

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())


        // we will treat linear layout as an array  // for color selecting
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint =
            linearLayoutPaintColors[1] as ImageButton // give the view at position 1
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            ) // tell us which color is selected
        )


        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)// button for undo function
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }
        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        //Adding an click event to image button for selecting the image from gallery.)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

        // to convert view to bitmap, that`s how it will be stored because view cant be stored directly
        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            //check if permission is allowed
            if (isReadStorageAllowed()) {

                // when saving occur progress bar will be visible
                showProgressDialog()

                //launch a coroutine block
                lifecycleScope.launch {
                    //reference the frame layout
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss() // after selecting brush dialog will disappear
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss() // after selecting brush dialog will disappear
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss() // after selecting brush dialog will disappear
        }
        brushDialog.show()

    }

    // to add color picker https://www.geeksforgeeks.org/how-to-create-a-color-picker-tool-in-android-using-color-wheel-and-slider/
    // all colors will be like onClick event
    fun paintClicked(view: View) {
        //Toast.makeText(this,"clicked",Toast.LENGTH_LONG).show()
        if (view !== mImageButtonCurrentPaint) {
            val imageButton =
                view as ImageButton //  making sure only imageButton that is our color button
            // should assign to it
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag) // will set the color

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_pressed
                ) // tell us which color is selected
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                ) // will unselect the previously selected color
            )
            mImageButtonCurrentPaint =
                view // we making the shift.. if not add this line all the colors will act like pallet pressed.
        }

    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    //method to requestStorage permission
    private fun requestStoragePermission() {
        //Check if the permission was denied and show rationale

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) // only work if camera is denied so modify accordingly

        ) {
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog(
                "Kids Drawing App", "Kids Drawing App " +
                        "needs to Access Your External Storage"
            )
        } else {
            // You can directly ask for the permission.
            //  The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss() // if user cancel than dismiss the dialog
            }
        builder.create().show() // create this dialog and show it
    }

    private fun getBitmapFromView(view: View): Bitmap {
        // defining the bitmap with the same size as the view.
        // createBitmap : returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // binding canvas to view
        val canvas = Canvas(returnedBitmap)

        // getting the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            // if has the background
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {

            if (mBitmap != null) {
                try {
                    // this thread will not run on UI thread
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        getExternalFilesDir(null),
                        "KidsDrawingApp_" + (System.currentTimeMillis() / 1000) + ".png"
                    )
//                    val f = File(
//                        externalCacheDir?.absoluteFile.toString()
//                                + File.separator + "kidsDrawingApp_" + System.currentTimeMillis() / 1000 +
//                                ".png"
//                    )
                    // to give each file different name we use System.currentTimeMillis()/1000
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath

                    // this will run in UI thread
                    runOnUiThread {
                        // when saving done progressBar will hide or cancel
                        cancelProgressDialog()

                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully : $result",
                                Toast.LENGTH_SHORT
                            ).show()


                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong file saving the file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }


            }
        }
        return result
    }

    private suspend fun shareBitmapFile(mBitmap: Bitmap?): String {
        var result = ""

        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)

                    val fName = File(
                        externalCacheDir,
                        "MyPaint_" + (System.currentTimeMillis() / 1000) + ".png"
                    )

                    val fOutputStream = FileOutputStream(fName)
                    fOutputStream.write(bytes.toByteArray())
                    fOutputStream.close()
                    result = fName.absolutePath

                    runOnUiThread {
                       // cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            //Toast.makeText(this@MainActivity, "Sharing $result",Toast.LENGTH_LONG ).show()
// to share image
                            shareImage(
                                FileProvider.getUriForFile(
                                    baseContext,
                                    "com.example.kidsdrawingapp.fileprovider",
                                    fName
                                )
                            )
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "File sharing failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)
        /* set the screen content from a layout resource.
        the resource will be inflated, adding all top-level views to the screen.*/

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        // start the dialog and display it on the screen
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }


    private fun shareImage(uri: Uri) {


        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.type = "image/jpeg"
        startActivity(Intent.createChooser(intent, "Share image via "))

    }

}