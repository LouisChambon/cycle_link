package com.example.cycle_link.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    modifier: Modifier = Modifier,
    onPublished: () -> Unit = {}
) {
    val auth      = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage   = FirebaseStorage.getInstance().reference

    val scope     = rememberCoroutineScope()
    val context   = LocalContext.current

    var title        by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }
    var priceText    by remember { mutableStateOf("") }
    var condition    by remember { mutableStateOf("neuf") }
    var catMenu      by remember { mutableStateOf(false) }
    var condMenu     by remember { mutableStateOf(false) }
    var category     by remember { mutableStateOf("Vélo") }

    var photoBitmap  by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri     by remember { mutableStateOf<Uri?>(null) }
    var isLoading    by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> photoBitmap = bitmap }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("Publier une annonce", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titre") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = {
                priceText = it.filter { ch -> ch.isDigit() || ch == '.' }
            },
            label = { Text("Prix ($)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Catégorie : ")
            Button(onClick = { catMenu = true }) { Text(category) }
            DropdownMenu(
                expanded = catMenu,
                onDismissRequest = { catMenu = false }
            ) {
                listOf("vélo", "électrique", "accessoire", "autre").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        category = opt; catMenu = false
                    })
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("État : ")
            Button(onClick = { condMenu = true }) { Text(condition) }
            DropdownMenu(
                expanded = condMenu,
                onDismissRequest = { condMenu = false }
            ) {
                listOf("neuf", "usé").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        condition = opt; condMenu = false
                    })
                }
            }
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                photoBitmap != null -> Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                photoUri != null -> Image(
                    painter = rememberAsyncImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> Text("Aucune image")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Caméra")
            }
            IconButton(onClick = {
                galleryLauncher.launch("image/*")
            }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galerie")
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isLoading,
            onClick = {
                val price = priceText.toDoubleOrNull() ?: 0.0
                val uid   = auth.currentUser?.uid ?: return@Button

                isLoading = true
                scope.launch {
                    try {
                        val downloadUrl = when {
                            photoUri != null -> {
                                val ref = storage.child("bikes_images/${UUID.randomUUID()}.jpg")
                                ref.putFile(photoUri!!).await()
                                ref.downloadUrl.await().toString()
                            }
                            photoBitmap != null -> {
                                val ref = storage.child("bikes_images/${UUID.randomUUID()}.jpg")
                                val baos = ByteArrayOutputStream().apply {
                                    photoBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, this)
                                }
                                ref.putBytes(baos.toByteArray()).await()
                                ref.downloadUrl.await().toString()
                            }
                            else -> ""
                        }

                        val data = mapOf(
                            "title"       to title,
                            "description" to description,
                            "price"       to price,
                            "category"    to category,
                            "condition"   to condition,
                            "imageUrl"    to downloadUrl,
                            "latitude"    to null,
                            "longitude"   to null,
                            "seller"      to "/users/$uid",
                            "status"      to "En cours d'approbation",
                            "createdAt"   to FieldValue.serverTimestamp()
                        )
                        firestore.collection("bikes")
                            .add(data)
                            .await()

                        Toast.makeText(context, "Annonce publiée !", Toast.LENGTH_SHORT).show()
                        onPublished()
                    }
                    catch(e: Exception) {
                        Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Publier")
        }
    }
}
