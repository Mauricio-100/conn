with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'val is_max_level: Boolean = false)' in line:
        lines.insert(i + 1, 'data class StoryResponse(\n')
        break

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.writelines(lines)
