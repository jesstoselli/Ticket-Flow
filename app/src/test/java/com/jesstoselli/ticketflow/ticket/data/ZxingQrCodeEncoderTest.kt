package com.jesstoselli.ticketflow.ticket.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZxingQrCodeEncoderTest {
    private val payload = "ticketflow:v1:ticket-1:purchase-1:event-1"

    @Test fun matrixIsSquareWithDarkAndLightModules() {
        val matrix = ZxingQrCodeEncoder.encodeToMatrix(payload, 256)

        assertEquals(matrix.width, matrix.height)
        assertTrue(matrix.width > 0)

        var hasDark = false
        var hasLight = false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) hasDark = true else hasLight = true
            }
        }
        assertTrue("QR deve conter módulos escuros e claros", hasDark && hasLight)
    }

    @Test fun encodedPayloadIsDecodable() {
        val matrix = ZxingQrCodeEncoder.encodeToMatrix(payload, 256)

        assertEquals(payload, decode(matrix))
    }

    private fun decode(matrix: BitMatrix): String {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val source = RGBLuminanceSource(width, height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return QRCodeReader().decode(bitmap).text
    }
}
