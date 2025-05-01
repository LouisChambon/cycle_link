package com.example.cycle_link.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cycle_link.model.BikeAd
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onBikeClick: (String) -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var bikeAds by remember { mutableStateOf<List<BikeAd>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // 1️⃣ Charger tous les docs "bikes"
            val snap = firestore.collection("bikes").get().await()

            // 2️⃣ Pour chaque annonce, on récupère aussi le user doc pour le nom / contact
            val temp = snap.documents.map { doc ->
                // Remplissage basique
                val basic = BikeAd(
                    id           = doc.id,
                    title        = doc.getString("title").orEmpty(),
                    description  = doc.getString("description").orEmpty(),
                    price        = doc.getDouble("price") ?: 0.0,
                    category     = doc.getString("category").orEmpty(),
                    condition    = doc.getString("condition").orEmpty(),
                    imageUrl     = doc.getString("imageUrl").orEmpty(),
                    latitude     = doc.getDouble("latitude"),
                    longitude    = doc.getDouble("longitude"),
                    seller       = doc.getString("seller").orEmpty(),
                    status       = doc.getString("status").orEmpty(),
                    createdAt    = doc.getTimestamp("createdAt")  // ou getDate()
                )
                // Extraction de l'UID depuis "/users/UID"
                val uid = basic.seller.substringAfterLast("/")
                // Lecture synchrone du doc user
                val userDoc = firestore.collection("users").document(uid).get().await()
                val name  = userDoc.getString("name").orEmpty()
                val email = userDoc.getString("email").orEmpty()
                val phone = userDoc.getString("phone").orEmpty()

                // On renvoie une copie enrichie
                basic.copy(
                    sellerName   = name,
                    contactEmail = email,
                    contactPhone = phone
                )
            }
            bikeAds = temp
        } catch (e: Exception) {
            // log ou Toast si besoin
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar   = {
            SmallTopAppBar(title = { Text("CycleLink") })
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                bikeAds.isEmpty() -> {
                    Text("Aucune annonce disponible",
                        modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        items(bikeAds) { bike ->
                            BikeCard(
                                bikeAd = bike,
                                onClick= { onBikeClick(bike.id) },
                                modifier = Modifier
                                    .width(180.dp)
                                    .aspectRatio(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BikeCard(
    bikeAd: BikeAd,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.clickable { onClick() }) {
        Column {
            AsyncImage(
                model               = bikeAd.imageUrl,
                contentDescription  = bikeAd.title,
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale        = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text       = bikeAd.title,
                    style      = MaterialTheme.typography.titleMedium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text       = "${bikeAd.price} €",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    text       = bikeAd.sellerName,
                    style      = MaterialTheme.typography.bodySmall,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }
        }
    }
}
