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
import androidx.compose.material.icons.filled.Close
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
    val pendingRef by viewModel.pendingPaymentReference.collectAsStateWithLifecycle()
    val pendingStatus by viewModel.pendingPaymentStatus.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("CDF") } // "CDF" ou "USD"
    var activeCheckoutResponse by remember { mutableStateOf<com.example.data.IddetPlusCheckoutResponse?>(null) }
    var showWidgetDialog by remember { mutableStateOf(false) }

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
                        if (pendingRef != null) {
                            viewModel.checkPaymentStatus(pendingRef!!) { st, isSuccess ->
                                if (isSuccess) {
                                    Toast.makeText(context, "🎉 Paiement confirmé ! Abonnement activé.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Statut : $st", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Statut actualisé", Toast.LENGTH_SHORT).show()
                        }
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
            // Suivi du paiement en cours (si une transaction Chariow a été initiée)
            if (pendingRef != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (pendingStatus) {
                            "success" -> Color(0xFFE8F5E9)
                            "failed" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pendingStatus == "success") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                Text("Paiement Confirmé !", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            } else if (pendingStatus == "failed") {
                                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                Text("Paiement Non Abouti", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Paiement en attente de validation", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Référence : $pendingRef",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = when (pendingStatus) {
                                "success" -> "Votre abonnement Iddet Plus et vos 500 crédits sont désormais actifs. Profitez de vos styles exclusifs !"
                                "failed" -> "La transaction Chariow a été annulée ou n'a pas pu aboutir. Vous pouvez réessayer."
                                else -> "Validation Mobile Money ou Carte en cours. Si vous avez validé le paiement sur Chariow, le serveur met à jour votre compte automatiquement."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.checkPaymentStatus(pendingRef!!) { st, isSuccess ->
                                        if (isSuccess) {
                                            Toast.makeText(context, "🎉 Félicitations ! Votre abonnement est actif !", Toast.LENGTH_LONG).show()
                                        } else if (st == "failed") {
                                            Toast.makeText(context, "Le paiement n'a pas abouti.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Paiement toujours en attente ($st)...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Vérifier", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val widget = activeCheckoutResponse?.widget ?: com.example.data.ChariowWidgetConfig(
                                        product_id = "prd_zs6iyq84",
                                        store_domain = "xnycggrc.mychariow.market",
                                        customer_email = user?.email
                                    )
                                    activeCheckoutResponse = com.example.data.IddetPlusCheckoutResponse(
                                        reference = pendingRef,
                                        amount = if (selectedCurrency == "USD") 1.25 else 2800.0,
                                        currency = selectedCurrency,
                                        widget = widget
                                    )
                                    showWidgetDialog = true
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Widget Chariow", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearPendingPayment() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Fermer", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

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
                            text = "Seulement 2800 CDF (~$1.25)/mois pour des avantages exclusifs !",
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
                    Text("Avantages Iddet Plus", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    BenefitRow("Styles de carte de profil premium (Neon, Gold, Diamond, Obsidian)")
                    BenefitRow("500 Crédits offerts chaque mois")
                    BenefitRow("Badge exclusif vérifié sur votre profil")
                    BenefitRow("Passerelle de paiement sécurisée Mobile Money & Cartes")
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
                    Text("S'abonner maintenant (2800 CDF / mois)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                        onSuccess = { Toast.makeText(context, "Style $style appliqué avec succès !", Toast.LENGTH_SHORT).show() },
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
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
                        contentDescription = "Paiement Sécurisé Chariow",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Paiement Sécurisé Chariow",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Abonnement Iddet Plus géré par la passerelle officielle du serveur (Mobile Money & Cartes).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Choix de la devise
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Devise & Moyen de paiement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedCurrency == "CDF",
                            onClick = { selectedCurrency = "CDF" },
                            label = { Text("CDF (2800 CDF)") },
                            leadingIcon = { if (selectedCurrency == "CDF") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedCurrency == "USD",
                            onClick = { selectedCurrency = "USD" },
                            label = { Text("USD ($1.25)") },
                            leadingIcon = { if (selectedCurrency == "USD") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Abonnement", fontWeight = FontWeight.Medium)
                    Text("Iddet Plus (30 jours)")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total à payer", fontWeight = FontWeight.Bold)
                    Text(if (selectedCurrency == "CDF") "2800 CDF" else "$1.25 USD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                val parsedInitialPhone = remember(user?.phoneNumber) {
                    com.example.utils.PhoneUtils.parse(user?.phoneNumber)
                }
                var countryCodeInput by remember(user?.phoneNumber) { mutableStateOf(parsedInitialPhone.countryCode) }
                var phoneInput by remember(user?.phoneNumber) { mutableStateOf(parsedInitialPhone.number) }
                var emailInput by remember(user?.email) { mutableStateOf(user?.email ?: "") }
                var phoneError by remember { mutableStateOf<String?>(null) }
                var emailError by remember { mutableStateOf<String?>(null) }

                if (user?.email.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 L'adresse email sert d'identifiant pour la validation automatique de votre paiement Chariow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Saisie du numéro de téléphone (Optionnel)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Numéro Mobile Money / Téléphone (Optionnel)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = countryCodeInput,
                            onValueChange = { countryCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Code") },
                            prefix = { Text("+") },
                            modifier = Modifier.width(95.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { 
                                phoneInput = it.filter { ch -> ch.isDigit() }
                                phoneError = null
                            },
                            label = { Text("Numéro") },
                            placeholder = { Text("Ex: 812345678") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = phoneError != null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (phoneError != null) {
                        Text(
                            text = phoneError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Optionnel (le widget Chariow vous demandera votre numéro si vous choisissez Mobile Money)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Saisie de l'email (exigé par le serveur Chariow)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Adresse Email (reçu Chariow)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it
                            emailError = null
                        },
                        label = { Text("Email") },
                        placeholder = { Text("Ex: nom@domaine.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = emailError != null,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                            emailError = "Un email valide est requis pour payer Iddet Plus."
                            Toast.makeText(context, "Un email est requis pour payer Iddet Plus.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        phoneError = null
                        emailError = null
                        isLoading = true

                        val cleanNum = com.example.utils.PhoneUtils.cleanNumber(phoneInput)

                        viewModel.createCheckoutSession(
                            email = emailInput.trim(),
                            phoneNumber = cleanNum.ifBlank { null },
                            countryCode = countryCodeInput,
                            currency = selectedCurrency,
                            onSuccess = { response ->
                                isLoading = false
                                showCheckoutSheet = false
                                activeCheckoutResponse = response
                                showWidgetDialog = true
                                Toast.makeText(context, "Session Chariow prête ! Réf: ${response.reference ?: "IDPLUS"}", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isLoading = false
                                if (err.contains("email", ignoreCase = true)) {
                                    emailError = err
                                }
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
                        val label = if (selectedCurrency == "CDF") "Procéder au paiement (2800 CDF)" else "Procéder au paiement ($1.25 USD)"
                        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        viewModel.simulateGooglePlayPurchase {
                            isLoading = false
                            showCheckoutSheet = false
                            Toast.makeText(context, "Félicitations ! Votre abonnement Iddet Plus est désormais actif 🎉", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activer via Google Play / Test direct", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showWidgetDialog && activeCheckoutResponse?.widget != null) {
        val checkout = activeCheckoutResponse!!
        com.example.ui.components.ChariowWidgetDialog(
            widgetConfig = checkout.widget!!,
            reference = checkout.reference,
            amount = checkout.amount,
            currency = checkout.currency ?: selectedCurrency,
            onDismiss = { showWidgetDialog = false },
            onVerifyStatus = {
                val ref = checkout.reference
                if (ref != null) {
                    viewModel.checkPaymentStatus(ref) { st, isSuccess ->
                        if (isSuccess) {
                            showWidgetDialog = false
                            Toast.makeText(context, "🎉 Paiement confirmé ! Votre statut Iddet Plus est actif.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Statut : $st (en cours de confirmation par le webhook Chariow)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onOpenExternal = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Impossible d'ouvrir le navigateur", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 14.sp)
    }
}
