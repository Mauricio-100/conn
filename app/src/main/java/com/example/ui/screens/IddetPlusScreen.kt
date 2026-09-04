package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.IddetViewModel
import com.android.billingclient.api.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IddetPlusScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    val status by viewModel.myIddetPlusStatus.collectAsStateWithLifecycle()
    val myCard by viewModel.myCard.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }
    var showPlayBillingSheet by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }

    var productDetails by remember { mutableStateOf<ProductDetails?>(null) }
    
    val billingClient = remember {
        BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    isPurchasing = false
                    showPlayBillingSheet = false
                    Toast.makeText(context, "Achat Google Play réussi ! Validation...", Toast.LENGTH_SHORT).show()
                    viewModel.simulateGooglePlayPurchase { 
                        // Once server validated (simulated) we grant access
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    isPurchasing = false
                } else {
                    isPurchasing = false
                }
            }
            .enablePendingPurchases()
            .build()
    }

    LaunchedEffect(billingClient) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                        .setProductList(
                            listOf(
                                QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId("iddet_plus_monthly")
                                    .setProductType(BillingClient.ProductType.SUBS)
                                    .build()
                            )
                        )
                        .build()

                    billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult2, productDetailsList ->
                        if (billingResult2.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                            productDetails = productDetailsList[0]
                        }
                    }
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    LaunchedEffect(Unit) {
        viewModel.loadIddetPlusData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iddet Plus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header / Status
            val isPremium = status?.is_iddet_plus == true
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isPremium) 
                            Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
                        else 
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer))
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = if (isPremium) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPremium) "Abonnement Actif" else "Passez au niveau supérieur",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isPremium) {
                        Text(
                            text = "Valable jusqu'au ${status?.expires_at?.take(10)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "Seulement $1.99/mois pour des avantages exclusifs !",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Benefits
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Avantages", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    BenefitRow("Styles de carte de profil premium (Neon, Gold, etc.)")
                    BenefitRow("500 Crédits offerts chaque mois")
                    BenefitRow("Badge exclusif sur votre profil")
                    BenefitRow("Soutien direct aux créateurs")
                }
            }

            if (!isPremium) {
                Button(
                    onClick = {
                        showPlayBillingSheet = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Text("S'abonner maintenant", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Style Picker
                Text("Personnaliser votre carte", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val styles = listOf("classic", "gold", "diamond", "neon", "obsidian")
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    styles.forEach { style ->
                        val isSelected = myCard?.card_style == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.updateCardStyle(
                                        style = style,
                                        onSuccess = { Toast.makeText(context, "Style mis à jour", Toast.LENGTH_SHORT).show() },
                                        onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(style.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    
    if (showPlayBillingSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isPurchasing) showPlayBillingSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Google Play",
                    tint = Color(0xFF00C853), // Google Play Green-ish
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Google Play",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Article", fontWeight = FontWeight.Medium)
                    Text("Iddet Plus (Mensuel)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Compte", fontWeight = FontWeight.Medium)
                    Text("ceoseshell@gmail.com")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Montant", fontWeight = FontWeight.Bold)
                    Text("1.99 €", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        isPurchasing = true
                        val activity = context as? android.app.Activity
                        if (activity != null && productDetails != null) {
                            val offerToken = productDetails?.subscriptionOfferDetails?.firstOrNull()?.offerToken
                            if (offerToken != null) {
                                val productDetailsParamsList = listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails!!)
                                        .setOfferToken(offerToken)
                                        .build()
                                )
                                val billingFlowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .build()
                                
                                val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                    isPurchasing = false
                                    Toast.makeText(context, "Erreur lors du lancement de Google Play", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isPurchasing = false
                                Toast.makeText(context, "Aucune offre trouvée pour ce produit", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Fallback pour le dev: l'article n'existe pas encore dans la Play Console
                            viewModel.simulateGooglePlayPurchase(
                                onSuccess = {
                                    isPurchasing = false
                                    showPlayBillingSheet = false
                                    Toast.makeText(context, "Achat Google Play simulé (Configuration Play Console manquante)", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPurchasing
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Acheter avec Google Play", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 14.sp)
    }
}
