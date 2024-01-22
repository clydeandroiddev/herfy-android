package com.herfy.util

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*
import com.herfy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QRHelper(val context: Context) {

    private val option by lazy {
        createQrVectorOptions {
            padding = .125f

            background {
                drawable =
                    ContextCompat.getDrawable(context, R.drawable.background_corner_white_8dp)
            }

            logo {
                drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                size = .20f
                padding = QrVectorLogoPadding.Natural(.2f)
                shape = QrVectorLogoShape.Default
            }
            colors {
                dark = QrVectorColor.Solid(ContextCompat.getColor(context, R.color.black))
                ball = QrVectorColor.Solid(
                    ContextCompat.getColor(context, R.color.black)
                )
            }

            shapes {
                darkPixel = QrVectorPixelShape.RoundCorners(.5f)
                ball = QrVectorBallShape.RoundCorners(.25f)
                frame = QrVectorFrameShape.RoundCorners(.25f)
            }
        }
    }

    suspend fun generateQR(data: String): Drawable {
        return withContext(Dispatchers.IO) {
            val qrData = QrData.Text(data)
            QrCodeDrawable(qrData, option)
        }
    }
}