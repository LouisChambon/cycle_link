package com.example.cycle_link.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cycle_link.model.BikeAd
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    bikeId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var bike by remember { mutableStateOf<BikeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(bikeId) {
        try {
            val doc = firestore.collection("bikes").document(bikeId).get().await()
            bike = BikeAd(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                description = doc.getString("description").orEmpty(),
                price = doc.getDouble("price") ?: 0.0,
                category = doc.getString("category").orEmpty(),
                condition = doc.getString("condition").orEmpty(),
                imageUrl = doc.getString("imageUrl").orEmpty(),
                latitude = doc.getDouble("latitude"),
                longitude = doc.getDouble("longitude"),
                seller = doc.getString("seller").orEmpty(),
                status = doc.getString("status").orEmpty(),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                sellerName = doc.getString("sellerName").orEmpty()
            )
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'annonce") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (bike != null) {
                    bike?.let { b ->
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = b.imageUrl,
                                contentDescription = b.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )

                            Text(text = b.title, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "${b.price} €",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val statusColor = when (b.status) {
                                "Validé" -> Color(0xFF4CAF50)   // vert
                                "En cours d'approbation" -> Color(0xFFFFA500)   // orange
                                "Refusé" -> Color(0xFFF44336)   // rouge
                                else -> Color.Gray
                            }
                            Text(
                                text = b.status,
                                color = statusColor,
                                style = MaterialTheme.typography.labelLarge
                            )

                            Divider()

                            Text("Description", style = MaterialTheme.typography.titleMedium)
                            Text(b.description, style = MaterialTheme.typography.bodyMedium)

                            Text(
                                "Catégorie : ${b.category}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "État : ${b.condition}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Divider()

                            Text("Vendeur", style = MaterialTheme.typography.titleMedium)
                            Text(b.sellerName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Text(
                        "Impossible de charger l'annonce",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
