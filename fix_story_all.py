with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    content = f.read()

# Replace all occurrences of data class StoryResponse( followed by newline and val id: String, back to just val id: String,
content = content.replace("data class StoryResponse(\n    val id: String,", "    val id: String,")

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.write(content)
