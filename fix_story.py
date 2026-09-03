with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val is_max_level: Boolean = false)\n    val id: String,',
    'val is_max_level: Boolean = false)\n\ndata class StoryResponse(\n    val id: String,'
)

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.write(content)

