package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Community
import com.example.ui.IddetViewModel
import com.example.ui.components.APP_CATEGORIES
import com.example.ui.components.CategoryInfo
import com.example.ui.components.MarkdownActfile
import com.example.ui.components.getCategoryById
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActfileComposerScreen(
    viewModel: IddetViewModel,
    onDismiss: () -> Unit,
    onPublish: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var tags by remember { mutableStateOf("") }
    
    // Helper function for inline styling
    val insertFormatting: (String, String) -> Unit = { prefix, suffix ->
        val currentValue = contentValue
        val selection = currentValue.selection
        val text = currentValue.text
        
        val selectedText = text.substring(selection.start, selection.end)
        val newText = StringBuilder(text)
            .replace(selection.start, selection.end, "$prefix$selectedText$suffix")
            .toString()
        
        val newSelectionStart = selection.start + prefix.length
        val newSelectionEnd = selection.end + prefix.length
        
        contentValue = currentValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)
        )
    }
    
    // Default category from APP_CATEGORIES
    val categories = APP_CATEGORIES
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.id ?: "@(fun)") }
    
    // Modes
    var isPreviewMode by remember { mutableStateOf(false) }
    
    // Limits
    val characterLimit = 1000
    val currentLength = contentValue.text.length
    
    // Communities & destination selectors
    val myCommunities by produceState<List<Community>>(initialValue = emptyList()) {
        viewModel.getMyCommunitiesFlow().collect { value = it }
    }
    var selectedCommunity by remember { mutableStateOf<Community?>(null) }
    var showDestinationMenu by remember { mutableStateOf(false) }
    
    // AI Suggestion & Verification
    var isAiSuggesting by remember { mutableStateOf(false) }
    var isCheckingSafety by remember { mutableStateOf(false) }
    var safetyError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    // Trigger AI category auto-suggestion as user writes
    LaunchedEffect(contentValue.text) {
        val text = contentValue.text
        if (text.length > 45 && com.example.utils.LocalAiManager.state.value == com.example.utils.AiModelState.READY) {
            delay(1500) // Debounce
            isAiSuggesting = true
            val suggestedName = com.example.utils.LocalAiManager.suggestCategory(text)
            if (suggestedName != null) {
                // Find matching category id
                val matched = categories.find { it.name.equals(suggestedName, ignoreCase = true) }
                if (matched != null) {
                    selectedCategory = matched.id
                }
            }
            isAiSuggesting = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().testTag("actfile_composer_screen"),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("composer_close_button")) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                    title = {
                        // Contextual Header: Destination Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .clickable { showDestinationMenu = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("composer_destination_selector")
                        ) {
                            Icon(
                                imageVector = if (selectedCommunity != null) Icons.Default.Group else Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCommunity?.let { "c/${it.slug}" } ?: "Mon Journal",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    },
                    actions = {
                        // Publish button
                        val isContentValid = currentLength in 1..characterLimit
                        Button(
                            onClick = {
                                val textContent = contentValue.text
                                if (textContent.isNotBlank() && isContentValid) {
                                    isCheckingSafety = true
                                    safetyError = null
                                    scope.launch {
                                        val isSafe = com.example.utils.LocalAiManager.checkAppropriate(textContent)
                                        if (isSafe) {
                                            // Append community tag if destination is a community
                                            val finalContent = if (selectedCommunity != null) {
                                                "$textContent\n\n@c/${selectedCommunity!!.slug}"
                                            } else textContent
                                            onPublish(finalContent, tags, selectedCategory)
                                        } else {
                                            safetyError = "⚠️ S3 AI a détecté que ce contenu enfreint les règles d'utilisation de gopu.inc."
                                        }
                                        isCheckingSafety = false
                                    }
                                }
                            },
                            enabled = isContentValid && !isCheckingSafety,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("composer_publish_button")
                        ) {
                            if (isCheckingSafety) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Publier",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Category Selection (Badges / Horizontal TagChips)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sélectionner une catégorie",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAiSuggesting) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "AI suggère...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories, key = { it.id }) { cat ->
                                val isSelected = cat.id == selectedCategory
                                val chipBg = if (isSelected) cat.color else Color.Transparent
                                val chipBorder = if (isSelected) Color.Transparent else cat.color.copy(alpha = 0.5f)
                                val contentColor = if (isSelected) Color.White else cat.color

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedCategory = cat.id }
                                        .testTag("composer_category_${cat.name}"),
                                    color = chipBg,
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = cat.emoji, fontSize = 14.sp)
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = contentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Mode Toggle Tab (Éditer vs Aperçu)
                    TabRow(
                        selectedTabIndex = if (isPreviewMode) 1 else 0,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
                    ) {
                        Tab(
                            selected = !isPreviewMode,
                            onClick = { isPreviewMode = false },
                            modifier = Modifier.testTag("composer_tab_edit")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Éditer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Tab(
                            selected = isPreviewMode,
                            onClick = { isPreviewMode = true },
                            modifier = Modifier.testTag("composer_tab_preview")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Aperçu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Body Input Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isPreviewMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (contentValue.text.isBlank()) {
                                    Text(
                                        text = "Rien à prévisualiser pour l'instant. Commencez à taper en mode Éditer !",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                } else {
                                    MarkdownActfile(content = contentValue.text)
                                }
                            }
                        } else {
                            // Raw text editor (Borderless, comfortable typography)
                            OutlinedTextField(
                                value = contentValue,
                                onValueChange = {
                                    if (it.text.length <= characterLimit + 50) { // Allow slight overflow for visual feedback
                                        contentValue = it
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("composer_text_input"),
                                placeholder = {
                                    Text(
                                        text = "Exprimez-vous ici... Utilisez le Markdown pour formater (gras, listes, code, citations, etc.)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    errorBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Safety / moderation error banner if active
                    safetyError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Footer Tags Input Field
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("composer_tags_input"),
                        placeholder = { Text("Tags séparés par virgule (ex: tech, gopu, fun)") },
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Toolbar + Gauge Progress Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Markdown tool bar shortcuts
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { insertFormatting("**", "**") },
                                    enabled = !isPreviewMode,
                                    modifier = Modifier.testTag("format_bold")
                                ) {
                                    Icon(Icons.Default.FormatBold, contentDescription = "Gras")
                                }
                                IconButton(
                                    onClick = { insertFormatting("*", "*") },
                                    enabled = !isPreviewMode,
                                    modifier = Modifier.testTag("format_italic")
                                ) {
                                    Icon(Icons.Default.FormatItalic, contentDescription = "Italique")
                                }
                                IconButton(
                                    onClick = { insertFormatting("[", "](url)") },
                                    enabled = !isPreviewMode,
                                    modifier = Modifier.testTag("format_link")
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = "Lien")
                                }
                                IconButton(
                                    onClick = { insertFormatting("> ", "") },
                                    enabled = !isPreviewMode,
                                    modifier = Modifier.testTag("format_quote")
                                ) {
                                    Icon(Icons.Default.FormatQuote, contentDescription = "Citation")
                                }
                                IconButton(
                                    onClick = { insertFormatting("`", "`") },
                                    enabled = !isPreviewMode,
                                    modifier = Modifier.testTag("format_code")
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = "Code en ligne")
                                }
                            }

                            // Dynamic Character Counter with Circular Gauge (M3 style)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val ratio = currentLength.toFloat() / characterLimit.toFloat()
                                val gaugeColor = when {
                                    ratio > 0.85f -> MaterialTheme.colorScheme.error
                                    ratio > 0.5f -> Color(0xFFF59E0B) // Amber
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    // Background track
                                    CircularProgressIndicator(
                                        progress = { 1f },
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        strokeWidth = 3.dp
                                    )
                                    // Active gauge
                                    CircularProgressIndicator(
                                        progress = { ratio.coerceAtMost(1f) },
                                        color = gaugeColor,
                                        strokeWidth = 3.dp
                                    )
                                }

                                Text(
                                    text = "$currentLength/$characterLimit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentLength > characterLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Dropdown Contextual Picker for Destination (Mon Journal / Communities list)
                if (showDestinationMenu) {
                    AlertDialog(
                        onDismissRequest = { showDestinationMenu = false },
                        title = {
                            Text(
                                text = "Où publier ?",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                            ) {
                                // Mon Journal selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedCommunity == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                        .clickable {
                                            selectedCommunity = null
                                            showDestinationMenu = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Mon Journal (Public)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Mes Communautés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                if (myCommunities.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Vous n'avez rejoint aucune communauté.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                    ) {
                                        myCommunities.forEach { com ->
                                            val isSelected = selectedCommunity?.id == com.id
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                                    .clickable {
                                                        selectedCommunity = com
                                                        showDestinationMenu = false
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val icon = com.iconUrl ?: "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=50&q=80"
                                                    AsyncImage(
                                                        model = icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = com.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "c/${com.slug}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDestinationMenu = false }) {
                                Text("Fermer")
                            }
                        }
                    )
                }
            }
        }
    }
}
