package com.example.tv_app

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import com.example.tv_app.ui.theme.TV_AppTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TV_AppTheme {
                val pagerState = rememberPagerState()
                val imageUrls = imageSwiper()
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(7000)
                        val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                        pagerState.scrollToPage(nextPage)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            count = imageUrls.size,
                            state = pagerState,
                            key = { imageUrls[it] },
                        )
                        { index ->
                            AsyncImage(
                                model = imageUrls[index],
                                contentDescription = "Events",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                val myRef = Firebase.database.getReference("Event_counter")

                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.getValue<Long>()?.toInt()
                        if (imageUrls.size != value && imageUrls.isNotEmpty()) {
                            Log.w(TAG, "Nombre d'events a changé : " + value)
                            restartActivity()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Failed to read value.", error.toException())
                    }
                })
            }
        }
    }

    private fun restartActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }

}

@Composable
fun imageSwiper(): List<String> {
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    DisposableEffect(Unit) {
        val storageRef = Firebase.storage.reference.child("Events")
        val imageList = mutableListOf<String>()

        storageRef.listAll().addOnSuccessListener { result ->
            result.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    imageList.add(uri.toString())
                    if (imageList.size == result.items.size) {
                        imageUrls = imageList.toList()
                    }
                }.addOnFailureListener { exception ->
                    Log.e("Download", "Failed to retrieve download URL: $exception")
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("Download", "Failed to list files: $exception")
        }

        onDispose {
        }
    }

    return imageUrls
}


@Preview(showBackground = true)
@Composable
fun ImageSwiperPreview() {
    TV_AppTheme {
        imageSwiper()
    }
}