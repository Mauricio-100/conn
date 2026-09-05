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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.IddetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IddetPlusScreen(viewModel: IddetViewModel, navController: NavController) {
    val context = LocalContext.current
    val status by viewModel.myIddetPlusStatus.collectAsStateWithLifecycle()
    val myCard by viewModel.myCard.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

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
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadIddetPlusData()
                        Toast.makeText(context, "Statut actualisé", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
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
                            text = "Valable jusqu'au ${status?.expires_at?.take(10) ?: "renouvellement"}",
                            color = Color.White.copy(alpha = 0.9f),
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
                        showCheckoutSheet = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Text("S'abonner maintenant ($1.99 / mois)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    
    if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isLoading) showCheckoutSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Paiement Sécurisé",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Paiement Sécurisé",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Le paiement est géré directement et de façon sécurisée par le serveur (Cartes bancaires, Mobile Money...).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Abonnement", fontWeight = FontWeight.Medium)
                    Text("Iddet Plus (Mensuel)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Montant", fontWeight = FontWeight.Bold)
                    Text("1.99 USD", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        isLoading = true
                        viewModel.createCheckoutSession(
                            onSuccess = { checkoutUrl ->
                                isLoading = false
                                showCheckoutSheet = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Redirection vers la page de paiement...", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = { err ->
                                isLoading = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                if (err.contains("connecter", ignoreCase = true) || err.contains("authenticated", ignoreCase = true)) {
                                    showCheckoutSheet = false
                                    navController.navigate("auth")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Procéder au paiement ($1.99)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
