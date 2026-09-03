import re

with open('app/src/main/java/com/example/data/IddetRepository.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'LevelInfo\("([^"]+)", (\d+)\)',
    r'LevelInfo(0, 0, "\1", null, null, \2, 0.0, false)',
    content
)

with open('app/src/main/java/com/example/data/IddetRepository.kt', 'w') as f:
    f.write(content)
