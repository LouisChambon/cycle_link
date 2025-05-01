package com.example.cycle_link.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.cycle_link.model.BikeAd
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onBikeClick: (String) -> Unit = {}
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var bikeAds   by remember { mutableStateOf<List<BikeAd>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query     by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val snap = firestore.collection("bikes").get().await()
            bikeAds = snap.documents.map { doc ->
                BikeAd(
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
                    createdAt    = doc.getTimestamp("createdAt"),
                    sellerName   = doc.getString("sellerName").orEmpty(),
                    contactEmail = doc.getString("contactEmail").orEmpty(),
                    contactPhone = doc.getString("contactPhone").orEmpty()
                )
            }
        } catch(_ : Exception) {
        } finally {
            isLoading = false
        }
    }

    // Filtrer
    val filtered = if (query.isBlank()) bikeAds
    else bikeAds.filter {
        it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
    }

    // Trier par date
    val sorted = filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
    val recent = sorted.take(4)    // <-- 4 annonces récentes max
    val popular= sorted.drop(4)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar   = {
            SmallTopAppBar(title = { Text("CycleLink") })
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(inner)
        ) {
            OutlinedTextField(
                value            = query,
                onValueChange    = { query = it },
                placeholder      = { Text("Rechercher...") },
                leadingIcon      = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Aucune annonce trouvée")
                }
                return@Column
            }

            Text(
                "Annonces récentes",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
            LazyRow(
                contentPadding        = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recent) { bike ->
                    BikeCard(
                        bikeAd   = bike,
                        onClick  = { onBikeClick(bike.id) },
                        modifier = Modifier
                            .width(180.dp)
                            .aspectRatio(0.8f)
                    )
                }
            }

            Text(
                "Populaires",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )
            LazyRow(
                contentPadding        = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(popular) { bike ->
                    BikeCard(
                        bikeAd   = bike,
                        onClick  = { onBikeClick(bike.id) },
                        modifier = Modifier
                            .width(180.dp)
                            .aspectRatio(0.8f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun BikeCard(
    bikeAd: BikeAd,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
    ) {
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
                    text      = bikeAd.title,
                    style     = MaterialTheme.typography.titleMedium,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Text(
                    text      = "${bikeAd.price} €",
                    style     = MaterialTheme.typography.labelLarge,
                    color     = MaterialTheme.colorScheme.primary
                )
                Text(
                    text      = bikeAd.sellerName,
                    style     = MaterialTheme.typography.bodySmall,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
            }
        }
    }
}
