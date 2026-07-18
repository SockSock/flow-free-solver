package com.socksock.flowfreesolver.model

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
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
        val rgb = Mat()
        Imgproc.cvtColor(image, rgb, Imgproc.COLOR_RGBA2RGB)
        val grey = Mat()
        Imgproc.cvtColor(rgb, grey, Imgproc.COLOR_RGB2GRAY)

        val mask = Mat()
        Imgproc.threshold(grey, mask, 20.0, 255.0, Imgproc.THRESH_BINARY)

        val fullWidth = mask.cols()
        val fullHeight = mask.rows()

        fun rowBrightFraction(y: Int): Double {
            val row = mask.row(y)
            val sum = org.opencv.core.Core.sumElems(row).`val`[0] / 255.0
            return sum / fullWidth
        }

        val rowThreshold = 0.9
        val candidateRows = (0 until fullHeight).filter { rowBrightFraction(it) > rowThreshold }

        if (candidateRows.isEmpty()) {
            rgb.release(); grey.release(); mask.release()
            return Pair(0, 0)
        }

        fun clusterPositions(candidates: List<Int>, minSpacing: Int): List<Int> {
            val positions = mutableListOf<Int>()
            var clusterStart = candidates[0]
            var prev = candidates[0]
            for (idx in candidates.drop(1)) {
                if (idx - prev > minSpacing) {
                    positions.add((clusterStart + prev) / 2)
                    clusterStart = idx
                }
                prev = idx
            }
            positions.add((clusterStart + prev) / 2)
            return positions
        }

        fun pruneOutlierPositions(positions: List<Int>): List<Int> {
            if (positions.size < 3) return positions
            val gaps = positions.zipWithNext { a, b -> b - a }
            val sortedGaps = gaps.sorted()
            val medianGap = sortedGaps[sortedGaps.size / 2].toDouble()

            val pruned = positions.toMutableList()
            while (pruned.size > 2) {
                val firstGap = pruned[1] - pruned[0]
                val lastGap = pruned[pruned.size - 1] - pruned[pruned.size - 2]
                val firstBad = firstGap > medianGap * 1.5 || firstGap < medianGap * 0.5
                val lastBad = lastGap > medianGap * 1.5 || lastGap < medianGap * 0.5
                if (lastBad) {
                    pruned.removeAt(pruned.size - 1)
                } else if (firstBad) {
                    pruned.removeAt(0)
                } else {
                    break
                }
            }
            return pruned
        }

        val horizontalPositions = pruneOutlierPositions(clusterPositions(candidateRows, maxOf(1, fullHeight / 100)))

        if (horizontalPositions.size < 2) {
            rgb.release(); grey.release(); mask.release()
            return Pair(0, 0)
        }

        val gridTop = horizontalPositions.first()
        val gridBottom = horizontalPositions.last()
        val gridRowSpan = gridBottom - gridTop
        val subMask = Mat(mask, org.opencv.core.Rect(0, gridTop, fullWidth, gridRowSpan))

        fun colBrightFraction(x: Int): Double {
            val col = subMask.col(x)
            val sum = org.opencv.core.Core.sumElems(col).`val`[0] / 255.0
            return sum / gridRowSpan
        }

        val colThreshold = 0.9
        val candidateCols = (0 until fullWidth).filter { colBrightFraction(it) > colThreshold }

        if (candidateCols.isEmpty()) {
            rgb.release(); grey.release(); mask.release(); subMask.release()
            return Pair(0, 0)
        }

        val verticalPositions = pruneOutlierPositions(clusterPositions(candidateCols, maxOf(1, fullWidth / 100)))

        val numRows = maxOf(0, horizontalPositions.size - 1)
        val numColumns = maxOf(0, verticalPositions.size - 1)

        rgb.release()
        grey.release()
        mask.release()
        subMask.release()

        return Pair(numRows, numColumns)
    }
}