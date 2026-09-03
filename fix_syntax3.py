with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    content = f.read()

content = content.replace(")\ndata class LevelsTableResponse(val levels: List<LevelInfo>)\ndata class StoryResponse(", ")\n\ndata class StoryResponse(")

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.write(content)
