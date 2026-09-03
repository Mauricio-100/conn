import re

with open('app/src/main/java/com/example/data/IddetRepository.kt', 'r') as f:
    content = f.read()

# The inserted block is right after `class IddetRepository(` up to before `    private val userDao:`
match = re.search(r'(class IddetRepository\()(\s*suspend fun getMyIddetPlusStatus.*?createIddetPlusCheckout\("Bearer \$token", IddetPlusCheckoutRequest\("USD"\)\)\s*\}\s*)(    private val userDao:)', content, flags=re.DOTALL)

if match:
    # remove it from the constructor and put it after `) {`
    prefix = content[:match.start(2)]
    suffix = content[match.end(2):]
    
    # suffix has `private val userDao: ... ) {`
    # let's find `) {`
    paren_idx = suffix.find(') {')
    if paren_idx != -1:
        new_suffix = suffix[:paren_idx + 3] + "\n" + match.group(2) + suffix[paren_idx + 3:]
        content = prefix + new_suffix

with open('app/src/main/java/com/example/data/IddetRepository.kt', 'w') as f:
    f.write(content)

