package com.example.cycle_link.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.cycle_link.model.BikeAd
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.*

private const val TAG = "BikeDetail"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeDetailScreen(
    bikeId: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val context   = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val fused     = remember { LocationServices.getFusedLocationProviderClient(context) }

    var bike        by remember { mutableStateOf<BikeAd?>(null) }
    var isLoading   by remember { mutableStateOf(true) }
    var error       by remember { mutableStateOf<String?>(null) }
    var isFavorite  by remember { mutableStateOf(false) }
    var distanceKm  by remember { mutableStateOf<Int?>(null) }

    val uid = FirebaseAuth.getInstance().currentUser!!.uid

    /* ---------- permission localisation ---------- */
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc ?: return@addOnSuccessListener
                bike?.let { b ->
                    distanceKm = safeDistanceKm(loc.latitude, loc.longitude, b.latitude, b.longitude)
                }
            }
        }
    }

    /* ---------- on load ---------- */
    LaunchedEffect(bikeId) {
        try {
            // est-ce qu’on a déjà liké ?
            isFavorite = firestore.collection("users")
                .document(uid)
                .collection("favorites")
                .document(bikeId)
                .get()
                .await()
                .exists()

            // annonce + user
            val doc = firestore.collection("bikes").document(bikeId).get().await()
            val sellerPath = doc.getString("seller").orEmpty()                 // "/users/uid"
            val sellerName = firestore.document(sellerPath).get().await()
                .getString("name").orEmpty()

            bike = BikeAd(
                id          = doc.id,
                title       = doc.getString("title").orEmpty(),
                description = doc.getString("description").orEmpty(),
                price       = doc.getDouble("price") ?: 0.0,
                category    = doc.getString("category").orEmpty(),
                condition   = doc.getString("condition").orEmpty(),
                imageUrl    = doc.getString("imageUrl").orEmpty(),
                latitude    = doc.getDouble("latitude"),
                longitude   = doc.getDouble("longitude"),
                seller      = sellerPath,
                status      = doc.getString("status").orEmpty(),
                createdAt   = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                sellerName  = sellerName
            )
        } catch (e: Exception) {
            error = "Impossible de charger l'annonce : ${e.localizedMessage}"
        } finally { isLoading = false }

        // dès qu’on a l’annonce on peut tenter la localisation
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc ?: return@addOnSuccessListener
                bike?.let { b ->
                    distanceKm = safeDistanceKm(loc.latitude, loc.longitude, b.latitude, b.longitude)
                }
            }
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /* ---------- UI ---------- */
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Détails") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val favRef = firestore
                            .collection("users").document(uid)
                            .collection("favorites").document(bikeId)
                        if (isFavorite) favRef.delete()
                        else favRef.set(mapOf("addedAt" to FieldValue.serverTimestamp()))
                        isFavorite = !isFavorite
                    }) {
                        Icon(
                            imageVector = if (isFavorite)
                                Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite)
                                MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.padding(16.dp))
                error != null -> Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                bike != null  -> BikeDetailContent(bike!!, distanceKm)
                else          -> Text("Annonce introuvable", Modifier.padding(16.dp))
            }
        }
    }
}

/* --------------------------- UI content --------------------------- */

@Composable
private fun BikeDetailContent(
    bikeAd    : BikeAd,
    distanceKm: Int?,
    modifier  : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model         = bikeAd.imageUrl,
            contentDescription = bikeAd.title,
            modifier      = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale  = ContentScale.Crop
        )

        Text(bikeAd.title, style = MaterialTheme.typography.headlineSmall)

        Text(
            "${bikeAd.price} €",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        distanceKm?.let {
            Text(
                "Distance : ${it} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val statusColor = when (bikeAd.status) {
            "Validé"                 -> Color(0xFF4CAF50)
            "En cours d'approbation" -> Color(0xFFFFA500)
            "Refusé"                 -> Color(0xFFF44336)
            else                     -> Color.Gray
        }
        Text(bikeAd.status, color = statusColor,
            style = MaterialTheme.typography.labelLarge)

        Divider()

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.description, style = MaterialTheme.typography.bodyMedium)

        Divider()

        Text("Vendeur", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.sellerName, style = MaterialTheme.typography.bodyMedium)
    }
}

/* ------------------------- Helpers ------------------------- */

/**
 *  Vérifie la validité des coordonnées.
 *  – si lat non ∈ [-90;90] ou lon non ∈ [-180;180] → probablement inversées ⇒ on swap
 *  – si toujours invalide ⇒ null ⇒ on ne calcule pas
 */
private fun safeDistanceKm(
    latUser : Double, lonUser: Double,
    latAdRaw: Double?, lonAdRaw: Double?
): Int? {
    if (latAdRaw == null || lonAdRaw == null) return null

    var latAd = latAdRaw
    var lonAd = lonAdRaw

    fun isValid(lat: Double, lon: Double) =
        lat in -90.0..90.0 && lon in -180.0..180.0

    if (!isValid(latAd, lonAd) && isValid(lonAd, latAd)) {
        Log.d(TAG, "Lat/Lon inversés détectés → swap")
        val tmp = latAd; latAd = lonAd; lonAd = tmp
    }

    if (!isValid(latAd, lonAd)) {
        Log.w(TAG, "Coordonnées invalides : lat=$latAd lon=$lonAd")
        return null
    }

    return haversineKm(latUser, lonUser, latAd, lonAd)
        .roundToInt()
}

/** Formule de Haversine (entrée en degrés, sortie km, Double) */
private fun haversineKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val R = 6371.0

    val φ1 = Math.toRadians(lat1)
    val φ2 = Math.toRadians(lat2)
    val Δφ = Math.toRadians(lat2 - lat1)
    val Δλ = Math.toRadians(lon2 - lon1)

    val a = sin(Δφ / 2).pow(2.0) +
            cos(φ1) * cos(φ2) *
            sin(Δλ / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
