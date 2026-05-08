import sys

filepath = 'src/main/java/esprit/fx/services/ServiceDisponibilite.java'

# Read as latin-1 to preserve raw bytes
with open(filepath, 'r', encoding='latin-1') as f:
    content = f.read()

replacements = [
    ('Ã©', 'é'),
    ('Ã¨', 'è'),
    ('Ãª', 'ê'),
    ('Ã§', 'ç'),
    ('Ã ', 'à'),
    ('Ã€', 'À'),
    ('Ã‰', 'É'),
    ('Ã®', 'î'),
    ('Ã´', 'ô'),
    ('Ã»', 'û'),
    ('â†'', '→'),
    ('âœ"', '✓'),
    ('âœ—', '✗'),
    ('âš ', '⚠'),
    ('â€™', "'"),
    ('â€œ', '"'),
    ('â€', '"'),
]

original = content
for bad, good in replacements:
    content = content.replace(bad, good)

count = sum(original.count(bad) for bad, good in replacements)
print(f'Found {count} encoding issues to fix')

# Write back as UTF-8
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print('Done - file saved as UTF-8')

# Verify
with open(filepath, 'r', encoding='utf-8') as f:
    verify = f.read()

remaining = sum(verify.count(bad) for bad, good in replacements)
print(f'Remaining issues after fix: {remaining}')
