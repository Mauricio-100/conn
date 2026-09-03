import re

with open('app/src/main/java/com/example/data/ApiService.kt', 'r') as f:
    content = f.read()

content = re.sub(
    r'(val is_max_level: Boolean = false\))(\s*val id: String,)',
    r'\1\n\ndata class StoryResponse(\2',
    content
)

with open('app/src/main/java/com/example/data/ApiService.kt', 'w') as f:
    f.write(content)

