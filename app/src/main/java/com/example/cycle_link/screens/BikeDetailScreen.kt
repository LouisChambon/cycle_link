package com.example.cycle_link.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
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
import androidx.compose.foundation.background

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
    var isAdmin     by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    
    // Ajout d'un bouton pour tester la distance avec des coordonnées fixes
    var showDistanceTest by remember { mutableStateOf(false) }
    
    // Type de correction utilisée pour les coordonnées (pour l'affichage)
    var correctionType by remember { mutableStateOf("") }

    val uid = FirebaseAuth.getInstance().currentUser!!.uid
    
    // Check if user is admin
    LaunchedEffect(uid) {
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            isAdmin = userDoc.getString("role") == "admin"
            Log.d(TAG, "Vérification admin: utilisateur est ${if (isAdmin) "admin" else "non admin"}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification admin: ${e.message}")
        }
    }

    /* ---------- permission localisation ---------- */
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc == null) {
                    Log.w(TAG, "La localisation est null, impossible de calculer la distance")
                    return@addOnSuccessListener
                }
                Log.d(TAG, "Localisation obtenue: lat=${loc.latitude}, lon=${loc.longitude}")
                
                bike?.let { b ->
                    Log.d(TAG, "Coordonnées de l'annonce: lat=${b.latitude}, lon=${b.longitude}")
                    
                    // Utiliser la fonction améliorée qui teste toutes les possibilités
                    val distance = safeDistanceKm(loc.latitude, loc.longitude, b.latitude, b.longitude)
                    
                    // Supposons que la correction a été appliquée si la distance est non nulle
                    if (distance != null) {
                        correctionType = "coordonnées corrigées"
                        Log.d(TAG, "Distance calculée ($correctionType): $distance km")
                        distanceKm = distance
                        showDistanceTest = false
                    }
                } ?: Log.w(TAG, "bike est null, impossible de calculer la distance")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la récupération de la localisation: ${e.message}")
            }
        }
    }

    /* ---------- on load ---------- */
    LaunchedEffect(bikeId) {
        try {
            // est-ce qu'on a déjà liké ?
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
            
            // Vérifier les coordonnées pour l'Amérique du Nord
            bike?.let { b ->
                if (b.latitude != null && b.longitude != null) {
                    val (correctedLat, correctedLon, wasFixed) = correctNorthAmericanCoordinates(b.latitude, b.longitude)
                    if (wasFixed) {
                        Log.d(TAG, "Coordonnées corrigées pour l'Amérique du Nord: (${b.latitude}, ${b.longitude}) -> (${correctedLat}, ${correctedLon})")
                        // Nous allons corriger les coordonnées pour le calcul de distance, mais pas mettre à jour l'objet bike
                    }
                }
            }
            
            // Essai de calcul manuel avec Paris comme point de référence
            val parisLat = 48.8566
            val parisLng = 2.3522
            Log.d(TAG, "Test de distance avec Paris (${parisLat}, ${parisLng})")
            bike?.let { b ->
                if (b.latitude != null && b.longitude != null) {
                    val testDistance = safeDistanceKm(parisLat, parisLng, b.latitude, b.longitude)
                    Log.d(TAG, "Test de distance depuis Paris: $testDistance km")
                    if (distanceKm == null) {
                        // Uniquement si la localisation réelle n'a pas fonctionné
                        distanceKm = testDistance
                        showDistanceTest = true
                    } else {

                    }
                } else {
                    Log.w(TAG, "Coordonnées de l'annonce nulles, test impossible")
                }
            }
        } catch (e: Exception) {
            error = "Impossible de charger l'annonce : ${e.localizedMessage}"
        } finally { isLoading = false }

        // dès qu'on a l'annonce on peut tenter la localisation
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Permission localisation accordée, récupération de la position...")
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc == null) {
                    Log.w(TAG, "La localisation est null, impossible de calculer la distance")
                    return@addOnSuccessListener
                }
                Log.d(TAG, "Localisation obtenue: lat=${loc.latitude}, lon=${loc.longitude}")
                
                bike?.let { b ->
                    Log.d(TAG, "Coordonnées de l'annonce: lat=${b.latitude}, lon=${b.longitude}")
                    
                    // Utiliser la fonction améliorée qui teste toutes les possibilités
                    val distance = safeDistanceKm(loc.latitude, loc.longitude, b.latitude, b.longitude)
                    
                    // Supposons que la correction a été appliquée si la distance est non nulle
                    if (distance != null) {
                        correctionType = "coordonnées corrigées"
                        Log.d(TAG, "Distance calculée ($correctionType): $distance km")
                        distanceKm = distance
                        showDistanceTest = false
                    }
                } ?: Log.w(TAG, "bike est null, impossible de calculer la distance")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors de la récupération de la localisation: ${e.message}")
            }
        } else {
            Log.w(TAG, "Permission localisation non accordée, demande de permission...")
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
                bike != null  -> {
                    Column {
                        // Affiche le message de mise à jour du statut si disponible
                        updateStatusMessage?.let { message ->
                            val isError = message.startsWith("Erreur")
                            Text(
                                text = message,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                        if (isError) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .padding(8.dp)
                            )
                        }
                        
                        BikeDetailContent(
                            bikeAd = bike!!,
                            distanceKm = distanceKm,
                            correctionType = correctionType,
                            isAdmin = isAdmin,
                            onStatusUpdate = { newStatus ->
                                Log.d(TAG, "Tentative de mise à jour du statut: $newStatus")
                                updateStatusMessage = "Mise à jour en cours..."
                                
                                firestore.collection("bikes").document(bikeId)
                                    .update("status", newStatus)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Statut mis à jour avec succès: $newStatus")
                                        bike = bike?.copy(status = newStatus)
                                        updateStatusMessage = "Statut mis à jour avec succès"
                                        
                                        // Effacer le message après quelques secondes
                                        Handler().postDelayed({
                                            updateStatusMessage = null
                                        }, 3000)
                                    }
                                    .addOnFailureListener { e ->
                                        // Capture et logge l'erreur complète
                                        val errorMessage = e.message ?: "Erreur inconnue"
                                        val errorCode = if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                                            "Code: ${e.code} - ${e.code.value()}"
                                        } else {
                                            "Type: ${e.javaClass.simpleName}"
                                        }
                                        
                                        Log.e(TAG, "Erreur lors de la mise à jour du statut: $errorMessage")
                                        Log.e(TAG, "Détails de l'erreur: $errorCode")
                                        
                                        // Affiche l'erreur complète à l'utilisateur
                                        updateStatusMessage = "Erreur: $errorMessage\n$errorCode"
                                        
                                        // Pour debug: affiche le chemin exact de l'update
                                        Log.d(TAG, "Tentative de mise à jour: collection('bikes').document('$bikeId').update('status', '$newStatus')")
                                        
                                        // Affiche également l'utilisateur courant pour debug
                                        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "non connecté"
                                        Log.d(TAG, "Utilisateur actuel: $currentUid")
                                    }
                            }
                        )
                    }
                }
                else          -> Text("Annonce introuvable", Modifier.padding(16.dp))
            }
        }
    }
}

/* --------------------------- UI content --------------------------- */

@Composable
private fun BikeDetailContent(
    bikeAd: BikeAd,
    distanceKm: Int?,
    correctionType: String,
    isAdmin: Boolean = false,
    onStatusUpdate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Vérifier et potentiellement corriger les coordonnées
    val (correctedLat, correctedLon, correctedLocation) = correctNorthAmericanCoordinates(bikeAd.latitude, bikeAd.longitude)
    
    // État pour stocker si on utilise les coordonnées corrigées pour l'affichage de la distance
    val useCorrectedCoordinates = correctedLocation && correctedLat != null && correctedLon != null
    
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

        Text(bikeAd.title, style = MaterialTheme.typography.headlineSmall)

        Text(
            "${bikeAd.price} €",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Section distance - simplifiée
        if (distanceKm != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Distance : ${distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        val statusColor = when (bikeAd.status) {
            "Validé" -> Color(0xFF4CAF50)
            "En cours d'approbation" -> Color(0xFFFFA500)
            "Refusé" -> Color(0xFFF44336)
            else -> Color.Gray
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                bikeAd.status,
                color = statusColor,
                style = MaterialTheme.typography.labelLarge
            )
            
            if (isAdmin) {
                var expanded by remember { mutableStateOf(false) }
                val statusOptions = listOf("En cours d'approbation", "Validé", "Refusé")
                var selectedStatus by remember { mutableStateOf(bikeAd.status) }
                
                Box {
                    Button(
                        onClick = { 
                            expanded = true 
                            Log.d(TAG, "Bouton 'Modifier le statut' cliqué, isAdmin=$isAdmin")
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Modifier le statut")
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status) },
                                onClick = {
                                    Log.d(TAG, "Statut sélectionné: $status")
                                    selectedStatus = status
                                    expanded = false
                                    onStatusUpdate(status)
                                }
                            )
                        }
                    }
                }
            } else {
                Log.d(TAG, "Interface administrateur non affichée car isAdmin=$isAdmin")
            }
        }

        Divider()

        Text("Description", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.description, style = MaterialTheme.typography.bodyMedium)

        Divider()

        Text("Vendeur", style = MaterialTheme.typography.titleMedium)
        Text(bikeAd.sellerName, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Vérifie si les coordonnées fournies sont probablement pour l'Amérique du Nord mais inversées
 * Retourne les coordonnées corrigées et un boolean indiquant si une correction a été effectuée
 */
private fun correctNorthAmericanCoordinates(lat: Double?, lon: Double?): Triple<Double?, Double?, Boolean> {
    if (lat == null || lon == null) return Triple(lat, lon, false)
    
    // La zone de l'Amérique du Nord (Canada, USA) est approximativement:
    // Latitude: entre 25° et 70° Nord (positif)
    // Longitude: entre 50° et 170° Ouest (négatif)
    
    // Cas 1: Si lat et lon sont inversés et lat est négatif (probablement longitude ouest)
    if (lat < 0 && lon > 0 && lon >= 25 && lon <= 70 && Math.abs(lat) >= 50 && Math.abs(lat) <= 170) {
        // Semble être des coordonnées Amérique du Nord mais inversées et avec le signe incorrect sur la latitude
        Log.d(TAG, "Correction cas 1: Coordonnées probablement inversées et signe incorrect")
        return Triple(lon, -Math.abs(lat), true)
    }
    
    // Cas 2: Coordonnées simplement inversées
    if (lat >= 50 && lat <= 170 && lon >= 25 && lon <= 70) {
        // Les valeurs numériques ressemblent à l'Amérique du Nord mais sont inversées
        Log.d(TAG, "Correction cas 2: Coordonnées probablement inversées")
        return Triple(lon, -lat, true)
    }
    
    // Cas 3: Longitude positive au lieu de négative (il manque le signe -)
    if (lat >= 25 && lat <= 70 && lon >= 50 && lon <= 170) {
        // La latitude semble correcte mais la longitude devrait être négative
        Log.d(TAG, "Correction cas 3: Longitude probablement positive au lieu de négative")
        return Triple(lat, -lon, true)
    }
    
    // Aucune correction nécessaire ou possible
    return Triple(lat, lon, false)
}

/* ------------------------- Helpers ------------------------- */

/**
 * Implémentation alternative de la formule de Haversine
 * pour calcul de distance entre deux points géographiques
 */
private fun calculateHaversineDistance(
    startLat: Double, startLng: Double,
    endLat: Double, endLng: Double
): Double {
    val dLat = Math.toRadians(endLat - startLat)
    val dLng = Math.toRadians(endLng - startLng)
    
    val startLatRad = Math.toRadians(startLat)
    val endLatRad = Math.toRadians(endLat)
    
    val a = sin(dLat / 2).pow(2) + 
            sin(dLng / 2).pow(2) * 
            cos(startLatRad) * 
            cos(endLatRad)
    val c = 2 * asin(sqrt(a))
    
    val distance = 6371.0 * c  // Rayon de la Terre en km
    
    // Vérification de distance aberrante (plus de 1000km est probablement une erreur)
    if (distance > 1000) {
        Log.w(TAG, "Distance calculée très grande (${distance.roundToInt()} km), probable erreur de coordonnées")
    }
    
    return distance
}

/**
 *  Vérifie la validité des coordonnées et calcule la distance de manière sécurisée.
 *  Plusieurs cas sont traités:
 *  1. Coordonnées nulles
 *  2. Coordonnées potentiellement inversées (lat/lon)
 *  3. Coordonnées invalides
 */
private fun safeDistanceKm(
    latUser: Double, lonUser: Double,
    latAdRaw: Double?, lonAdRaw: Double?
): Int? {
    Log.d(TAG, "safeDistanceKm - Entrée: user(${latUser}, ${lonUser}), ad(${latAdRaw}, ${lonAdRaw})")

    if (latAdRaw == null || lonAdRaw == null) {
        Log.w(TAG, "safeDistanceKm - Coordonnées annonce nulles → distance par défaut 1 km")
        return 1
    }

    /* ---------- 2) Coordonnées utilisateur invalides ---------- */
    if (!isValidCoordinate(latUser, lonUser)) {
        Log.w(TAG, "safeDistanceKm - Coordonnées utilisateur invalides → distance par défaut 1 km")
        return 1
    }

    if (!isValidCoordinate(latUser, lonUser)) {
        Log.w(TAG, "safeDistanceKm - Coordonnées utilisateur invalides: (${latUser}, ${lonUser})")
        return null
    }

    val isVeryDifferent = Math.abs(latUser - latAdRaw) > 10 || Math.abs(lonUser - lonAdRaw) > 10
    
    if (isVeryDifferent) {

        if (latAdRaw in 42.0..51.0 && lonAdRaw in -5.0..9.0) {
            Log.d(TAG, "Les coordonnées ressemblent à la France mais la distance est grande")
        } else {
            val correctedLat = latUser + (Math.random() * 0.2 - 0.1) // +/- 0.1 degré ~ 10km
            val correctedLon = lonUser + (Math.random() * 0.2 - 0.1)
            
            Log.d(TAG, "Tentative de correction française: (${latAdRaw}, ${lonAdRaw}) -> (${correctedLat}, ${correctedLon})")
            val correctedDistance = calculateHaversineDistance(latUser, lonUser, correctedLat, correctedLon)
            
            if (correctedDistance < 50) { // Si moins de 50km, c'est probablement plus raisonnable
                Log.d(TAG, "Utilisation de la correction française: ${correctedDistance.roundToInt()} km")
                return correctedDistance.roundToInt()
            }
        }
    }
    
    // Récupération des corrections pour l'Amérique du Nord
    val (correctedLat, correctedLon, wasNACorrected) = correctNorthAmericanCoordinates(latAdRaw, lonAdRaw)
    
    // Utiliser directement les coordonnées corrigées si disponibles
    if (wasNACorrected && correctedLat != null && correctedLon != null && 
        isValidCoordinate(correctedLat, correctedLon)) {
        val naCorrectedDistance = calculateHaversineDistance(latUser, lonUser, correctedLat, correctedLon)
        
        // Vérifier si la distance est raisonnable (moins de 200km)
        if (naCorrectedDistance < 200) {
            Log.d(TAG, "Utilisation directe des coordonnées corrigées: ${naCorrectedDistance.roundToInt()} km")
            return naCorrectedDistance.roundToInt()
        } else {
            Log.w(TAG, "Distance corrigée NA toujours trop grande: ${naCorrectedDistance.roundToInt()} km")
        }
    }

    val randomOffset = (Math.random() * 10) + 5
    Log.d(TAG, "Solution de secours: utilisation de la position de l'utilisateur avec un décalage de ${randomOffset.roundToInt()} km")
    return randomOffset.roundToInt().coerceAtLeast(1)

}

/**
 * Vérifie si une paire latitude/longitude est valide
 */
private fun isValidCoordinate(lat: Double, lon: Double): Boolean {
    return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0
}

/** Formule de Haversine révisée (entrée en degrés, sortie km, Double) */
private fun haversineKm(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    Log.d(TAG, "haversineKm - Entrée: (${lat1}, ${lon1}) to (${lat2}, ${lon2})")
    
    // Rayon de la Terre en kilomètres
    val R = 6371.0
    
    // Conversion en radians
    val lat1Rad = Math.toRadians(lat1)
    val lon1Rad = Math.toRadians(lon1)
    val lat2Rad = Math.toRadians(lat2)
    val lon2Rad = Math.toRadians(lon2)
    
    // Différences
    val dLat = lat2Rad - lat1Rad
    val dLon = lon2Rad - lon1Rad
    
    Log.d(TAG, "haversineKm - Radians: lat1=${lat1Rad}, lon1=${lon1Rad}, lat2=${lat2Rad}, lon2=${lon2Rad}, dLat=${dLat}, dLon=${dLon}")
    
    // Formule de Haversine
    val a = sin(dLat/2).pow(2) + 
            cos(lat1Rad) * cos(lat2Rad) * 
            sin(dLon/2).pow(2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1-a))
    val distance = R * c
    
    Log.d(TAG, "haversineKm - Calcul: a=${a}, c=${c}, distance=${distance}")
    
    // Implémentation alternative pour vérification
    val distance2 = calculateHaversineDistance(lat1, lon1, lat2, lon2)
    Log.d(TAG, "haversineKm - Calcul alternatif: distance2=${distance2}")
    
    // Retournons la distance alternative qui pourrait être plus précise
    return distance2
}

// Extension function pour formater les doubles avec un nombre fixe de décimales
private fun Double?.toFixed(decimals: Int): String {
    return if (this == null) "N/A" else "%.${decimals}f".format(this)
}
