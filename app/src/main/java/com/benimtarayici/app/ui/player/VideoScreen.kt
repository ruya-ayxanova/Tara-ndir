package com.benimtarayici.app.ui.player

import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoScreen(baslik: String, uri: Uri, onGeri: () -> Unit) {
    BackHandler(enabled = true) { onGeri() }

    val context = LocalContext.current
    val video = remember {
        VideoView(context).apply {
            val kontrol = MediaController(context)
            kontrol.setAnchorView(this)
            setMediaController(kontrol)
            setVideoURI(uri)
            setOnPreparedListener { start() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(context, "Video oynatılamadı", Toast.LENGTH_LONG).show()
                true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { video.stopPlayback() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Text(
            text = baslik,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp)
        )

        AndroidView(
            factory = { video },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Text(
            text = "◀ Geri",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGeri() }
                .padding(16.dp)
        )
    }
}
