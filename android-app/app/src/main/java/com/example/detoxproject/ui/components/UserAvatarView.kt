package com.example.detoxproject.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.detoxproject.ui.theme.OfflineTextPrimary
import com.example.detoxproject.ui.theme.SoftBlue
import com.example.detoxproject.ui.theme.Typography
import java.io.File

@Composable
fun UserAvatarView(
    avatar: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    defaultName: String? = null
) {
    val isFilePath = remember(avatar) {
        avatar != null && (avatar.startsWith("/") || avatar.startsWith("file:") || File(avatar).exists())
    }
    val bitmap = remember(avatar, isFilePath) {
        if (isFilePath && avatar != null) {
            try {
                val cleanPath = avatar.removePrefix("file://")
                val f = File(cleanPath)
                if (f.exists() && f.length() > 0) {
                    BitmapFactory.decodeFile(cleanPath)
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SoftBlue),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!avatar.isNullOrBlank()) {
            Text(
                text = avatar,
                fontSize = (size.value * 0.55f).sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = (defaultName?.firstOrNull() ?: 'C').toString().uppercase(),
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OfflineTextPrimary
            )
        }
    }
}
