import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# I will find the start of the Column inside Scaffold
start_idx = content.find("verticalArrangement = Arrangement.spacedBy(16.dp)\n        ) {")

if start_idx != -1:
    end_idx = content.find("// Notifications & Sonneries Section")
    if end_idx != -1:
        # replace everything between start_idx and end_idx
        prefix = content[:start_idx + len("verticalArrangement = Arrangement.spacedBy(16.dp)\n        ) {\n")]
        suffix = content[end_idx:]
        
        # also we need to check if there's a second Marketplace Section inside Notifications
        suffix = re.sub(r'\s*// Marketplace Section.*?Icon\(\s*imageVector = Icons\.Default\.KeyboardArrowRight,\s*contentDescription = null,\s*tint = MaterialTheme\.colorScheme\.onSurfaceVariant\s*\)\s*\}\s*\}', '', suffix, flags=re.DOTALL)

        new_items = """
            // Iddet Plus Section
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("iddet_plus") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Iddet Plus",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Iddet Plus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Gérer votre abonnement et vos avantages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Marketplace Section
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("marketplace") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Marketplace",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Marketplace",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Publier et découvrir nos genres de projets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

"""
        content = prefix + new_items + suffix

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
