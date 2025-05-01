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
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bikeId) {
        isLoading = true
        try {
            // 1) Récupérer le doc de l'annonce
            val doc = firestore.collection("bikes")
                .document(bikeId)
                .get()
                .await()

            // 2) Extraire l'UID du vendeur depuis le champ "seller"
            //    ex: "/users/aK5VErRbZNMag6hgYxgbl32GMNe2"
            val sellerPath = doc.getString("seller").orEmpty()
            val sellerUid  = sellerPath.substringAfterLast('/')

            // 3) Récupérer le doc user pour le nom
            val userDoc = firestore.collection("users")
                .document(sellerUid)
                .get()
                .await()
            val sellerName = userDoc.getString("name").orEmpty()

            // 4) Construire l'objet BikeAd avec le sellerName
            bike = BikeAd(
                id           = doc.id,
                title        = doc.getString("title").orEmpty(),
                description  = doc.getString("description").orEmpty(),
                price        = doc.getDouble("price") ?: 0.0,
                category     = doc.getString("category").orEmpty(),
                condition    = doc.getString("condition").orEmpty(),
                imageUrl     = doc.getString("imageUrl").orEmpty(),
                latitude     = doc.getDouble("latitude"),
                longitude    = doc.getDouble("longitude"),
                seller       = sellerPath,
                status       = doc.getString("status").orEmpty(),
                createdAt    = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                sellerName   = sellerName
            )
        } catch (e: Exception) {
            error = "Impossible de charger l'annonce : ${e.localizedMessage}"
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
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                error != null -> {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                bike != null -> {
                    BikeDetailContent(bike!!)
                }
                else -> {
                    Text(
                        "Annonce introuvable",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BikeDetailContent(bikeAd: BikeAd, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = bikeAd.imageUrl,
            contentDescription = bikeAd.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Text(text = bikeAd.title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${bikeAd.price} €",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        val statusColor = when (bikeAd.status) {
            "Validé" -> Color(0xFF4CAF50)
            "En cours d'approbation" -> Color(0xFFFFA500)
            "Refusé" -> Color(0xFFF44336)
            else -> Color.Gray
        }
        Text(
            text = bikeAd.status,
            color = statusColor,
            style = MaterialTheme.typography.labelLarge
        )

        Divider()

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.description, style = MaterialTheme.typography.bodyMedium)

        Divider()

        Text("Vendeur", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.sellerName, style = MaterialTheme.typography.bodyMedium)
    }
}
