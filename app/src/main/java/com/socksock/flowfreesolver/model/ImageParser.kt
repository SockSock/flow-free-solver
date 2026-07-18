package com.socksock.flowfreesolver.model

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ImageParser {
    fun parseImage(bitmap: Bitmap) {
        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Utils.bitmapToMat(bitmap, mat)
        val coords = getRowsAndColsFromGrid(mat)
        Log.d("bag", "${coords.first} ${coords.second}")
        mat.release()
    }

    private fun getRowsAndColsFromGrid(image: Mat): Pair<Int, Int> {
        val grey = Mat()
        Imgproc.cvtColor(image, grey, Imgproc.COLOR_RGBA2GRAY)

        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            grey,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
        )

        val scaleH = maxOf(1, thresh.cols() / 30)
        val scaleV = maxOf(1, thresh.rows() / 30)

        val horizKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(scaleH.toDouble(), 1.0))
        val vertKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, scaleV.toDouble()))

        val horizLines = Mat()
        val vertLines = Mat()
        Imgproc.morphologyEx(thresh, horizLines, Imgproc.MORPH_OPEN, horizKernel,
            Point(-1.0, -1.0), 2)
        Imgproc.morphologyEx(thresh, vertLines, Imgproc.MORPH_OPEN, vertKernel, Point(-1.0, -1.0), 2)

        val contoursH = ArrayList<MatOfPoint>()
        val contoursV = ArrayList<MatOfPoint>()
        val hierarchy = Mat()

        Imgproc.findContours(horizLines, contoursH, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        Imgproc.findContours(vertLines, contoursV, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val minLineLength = 20

        val validHLines = contoursH.count { contour ->
            val points = contour.toArray()
            val width = points.maxOf { it.x } - points.minOf { it.x }

            width > minLineLength
        }

        val validVLines = contoursV.count { contour ->
            val points = contour.toArray()
            val height = points.maxOf { it.y } - points.minOf { it.y }

            height > minLineLength
        }

        val numRows = maxOf(0, validHLines - 1)
        val numColumns = maxOf(0, validVLines - 1)

        grey.release()
        thresh.release()
        horizKernel.release()
        vertKernel.release()
        horizLines.release()
        vertLines.release()
        hierarchy.release()

        return Pair(numRows, numColumns)
    }
}