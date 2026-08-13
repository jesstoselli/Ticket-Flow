package com.jesstoselli.ticketflow.ticket.domain

import androidx.compose.ui.graphics.ImageBitmap

/** Gera localmente o bitmap do QR Code de um ingresso a partir do payload já persistido. */
interface QrCodeEncoder {
    fun encode(payload: String, sizePx: Int): ImageBitmap
}
