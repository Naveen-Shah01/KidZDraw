package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var mDrawPath: CustomPath? = null // object of customPath class

    private var mCanvasBitmap: Bitmap? =
        null  // learn more about bitmap https://en.proft.me/2017/08/2/how-work-bitmap-android/
    private var mDrawPaint: Paint? =
        null  // the paint class holds the style and color information about how to draw
    private var mCanvasPaint: Paint? = null // instance of a canvas paint view
    private var mBrushSize: Float = 0.toFloat() // for stroke/brush size to draw on canvas
    private var color =
        Color.BLACK  // going to start color with black, variable to hold a color of the stroke


    private var canvas: Canvas? = null // for canvas
    private var mPaths =
        ArrayList<CustomPath>() // use to make the lines stay on screen/ an array list of paths
    private var mUndoPaths = ArrayList<CustomPath>()


    fun onClickUndo() {  // undo function
        if (mPaths.size > 0) {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))  // mPaths.removeAt(mPaths.size - 1) will also work no need to store in arrays.
            // we store in UndoPath array because just in case we have to use redo functionality.
            // we can also do  mUndoPaths.add(mPaths.removeLast())
            invalidate()
        }
    }
    fun onClickRedo(){   // for redo function
        if(mUndoPaths.size > 0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size-1))
            invalidate()
        }

    }

    init { // to setup the whole variable
        setUpDrawing()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color // color for mDrawPaint
        mDrawPaint!!.style = Paint.Style.STROKE // style of paint stroke= line
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND// how the beginning and End will be of stroke
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)  // creating paint object
        // no need after function setSizeForBrush, as we are going to set it in main activity
        // mBrushSize = 20.toFloat() // initial brush/troke size
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        canvas = Canvas(mCanvasBitmap!!)
    }


    // when draw
    // change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //bitmap where we want to draw our own bitmap mCanvasBitmap
        canvas.drawBitmap(
            mCanvasBitmap!!, 0F, 0F, mCanvasPaint
        )  // 0F,0F shows which position we are going to start

        for (path in mPaths) {
            // setting the width which we are going to draw
            mDrawPaint!!.strokeWidth = path.brushThickness
            // to set color of our custom path
            mDrawPaint!!.color = path.color

            canvas.drawPath(path, mDrawPaint!!) // as path name was path
        }
        // to draw out path
        if (!mDrawPath!!.isEmpty) {
            // setting the width which we are going to draw
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            // to set color of our custom path
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }

    // we want to draw when we touch the screen
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when (event?.action) {
            // when we press on the screen
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()

                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            // when we drag over the screen
            MotionEvent.ACTION_MOVE -> {
                if (touchY != null) {
                    if (touchX != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }
            // when we lift the finger
            MotionEvent.ACTION_UP -> {
                mPaths.add(mDrawPath!!) // and after this finally draw it in onDraw
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false

        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        // setting the new brush size with taking display pixel/dimension into consideration
        // that is adjust itself according to screen
        mBrushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics
        )
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(newColor: String) {
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path() {

    }

}