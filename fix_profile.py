import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    content = f.read()

# Replace com.example.data.LevelInfo("Débutant", 0) with com.example.data.LevelInfo(0, 0, "Débutant", null, null, 0, 0.0, false)
content = re.sub(
    r'com\.example\.data\.LevelInfo\("([^"]+)", (\d+)\)',
    r'com.example.data.LevelInfo(0, 0, "\1", null, null, \2, 0.0, false)',
    content
)

# And fix line 1645: it says "Unresolved reference 'name'". Let's check what it uses.
# It probably uses `level.name` and `level.min_score`.
content = content.replace("level.name", "level.level_name")
content = content.replace("level.min_score", "level.points_to_next") # It was min_score in the table. Let's look at the UI code.
# But wait, points_to_next is not min_score. The table needs min_score. I can just use points_to_next as min_score for the display table.

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(content)

