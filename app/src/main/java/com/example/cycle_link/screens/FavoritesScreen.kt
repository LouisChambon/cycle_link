package com.example.cycle_link.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.cycle_link.model.BikeAd
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    onBikeClick: (String) -> Unit = {}
) {
    val uid        = FirebaseAuth.getInstance().currentUser!!.uid
    val firestore  = remember { FirebaseFirestore.getInstance() }
    val scope      = rememberCoroutineScope()          // ← ➊

    var favorites by remember { mutableStateOf<List<BikeAd>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    /* ---------- écoute temps-réel des favoris ---------- */
    LaunchedEffect(uid) {
        firestore.collection("users").document(uid)
            .collection("favorites")
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id } ?: emptyList()

                // ← ➋  on passe en coroutine pour appeler la fonction suspend
                scope.launch {
                    favorites = loadAds(ids, firestore)   // loadAds est suspend
                    isLoading = false
                }
            }
    }

    /* ---------- UI…  (inchangée) ---------- */
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mes favoris", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            favorites.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Aucun favori", style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(bottom = 32.dp)
            ) {
                items(favorites, key = { it.id }) { bike ->
                    FavoriteItemCard(bike) { onBikeClick(bike.id) }
                }
            }
        }
    }
}

private suspend fun loadAds(
    ids: List<String>,
    firestore: FirebaseFirestore
): List<BikeAd> = ids.mapNotNull { id ->
    runCatching {
        val d = firestore.collection("bikes").document(id).get().await()
        if (!d.exists()) null else
            BikeAd(
                id           = d.id,
                title        = d.getString("title").orEmpty(),
                description  = d.getString("description").orEmpty(),
                price        = d.getDouble("price") ?: 0.0,
                category     = d.getString("category").orEmpty(),
                condition    = d.getString("condition").orEmpty(),
                imageUrl     = d.getString("imageUrl").orEmpty(),
                latitude     = d.getDouble("latitude"),
                longitude    = d.getDouble("longitude"),
                seller       = d.getString("seller").orEmpty(),
                status       = d.getString("status").orEmpty(),
                createdAt    = d.getTimestamp("createdAt"),
                sellerName   = d.getString("sellerName").orEmpty(),
                contactEmail = d.getString("contactEmail").orEmpty(),
                contactPhone = d.getString("contactPhone").orEmpty()
            )
    }.getOrNull()
}

@Composable
private fun FavoriteItemCard(
    bike: BikeAd,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter        = rememberAsyncImagePainter(bike.imageUrl),
                contentDescription = bike.title,
                modifier       = Modifier
                    .size(96.dp)
                    .aspectRatio(1f),
                contentScale   = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(bike.title, style = MaterialTheme.typography.titleMedium)
                Text("${bike.price} €", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text(bike.sellerName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
