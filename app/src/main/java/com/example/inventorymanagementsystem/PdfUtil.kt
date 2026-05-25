package com.example.inventorymanagementsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfUtil {
    fun createPdf(context: Context, fileName: String, title: String, meta: String, body: String): File {
        val cacheDir = context.cacheDir
        val outFile = File(cacheDir, "$fileName.pdf")
        if (outFile.exists()) outFile.delete()

        val fullText = StringBuilder()
            .append(title).append("\n\n")
            .append(meta).append("\n\n")
            .append(body)
            .toString()

        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = 12f * context.resources.displayMetrics.density
        }

        val pageWidth = (595 * context.resources.displayMetrics.density / 2.0f).toInt().coerceAtLeast(300)
        val staticLayout = StaticLayout.Builder.obtain(fullText, 0, fullText.length, paint, pageWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()

        val pageHeight = staticLayout.height + (40 * context.resources.displayMetrics.density).toInt()

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        canvas.translate(20f, 20f)
        staticLayout.draw(canvas)

        document.finishPage(page)

        FileOutputStream(outFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outFile
    }

    fun getUriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
