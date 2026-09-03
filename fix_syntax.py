with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    content = f.read()

content = content.replace("data class LevelsTableResponse(val levels: List<LevelInfo>))\n", "data class LevelsTableResponse(val levels: List<LevelInfo>)\n")
content = content.replace("data class LevelsTableResponse(val levels: List<LevelInfo>)\n)", "data class LevelsTableResponse(val levels: List<LevelInfo>)\n")

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.write(content)
