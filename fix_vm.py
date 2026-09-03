import re

with open('app/src/main/java/com/example/ui/IddetViewModel.kt', 'r') as f:
    content = f.read()

if content.startswith("import com.example.data.*\n"):
    content = content[len("import com.example.data.*\n"):]
    # insert after package
    content = content.replace("package com.example.ui\n", "package com.example.ui\n\nimport com.example.data.*\n")

with open('app/src/main/java/com/example/ui/IddetViewModel.kt', 'w') as f:
    f.write(content)
