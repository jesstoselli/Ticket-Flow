package com.jesstoselli.ticketflow.ticket.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.jesstoselli.ticketflow.ticket.domain.QrCodeEncoder
import javax.inject.Inject

class ZxingQrCodeEncoder @Inject constructor() : QrCodeEncoder {
    override fun encode(payload: String, sizePx: Int): ImageBitmap {
        val matrix = encodeToMatrix(payload, sizePx)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (matrix.get(x, y)) BLACK else WHITE
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap.asImageBitmap()
    }

    companion object {
        private const val BLACK = 0xFF000000.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()

        /** Parte pura (ZXing, sem Android) — isolada para testes em JVM. */
        fun encodeToMatrix(payload: String, sizePx: Int): BitMatrix {
            require(payload.isNotBlank()) { "Payload do QR não pode ser vazio" }
            require(sizePx > 0) { "Tamanho do QR deve ser positivo" }
            return QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                mapOf(EncodeHintType.MARGIN to 1),
            )
        }
    }
}
