#!/usr/bin/env python3
# -*- coding: utf-8 -*-

# Read the file
with open('src/main/java/esprit/fx/services/ServiceDisponibilite.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Define all replacements
replacements = {
    'Ã©': 'é',
    'Ã¨': 'è',
    'Ãª': 'ê',
    'Ã§': 'ç',
    'Ã ': 'à',
    'Ã´': 'ô',
    'Ã»': 'û',
    'Ã®': 'î',
    'Ã¯': 'ï',
    'Ã¹': 'ù',
    'Ã‰': 'É',
    'Ã€': 'À',
    'â†'': '→',
    'âœ"': '✓',
    'âœ—': '✗',
    'âš ': '⚠',
}

# Apply all replacements
for old, new in replacements.items():
    content = content.replace(old, new)

# Write back
with open('src/main/java/esprit/fx/services/ServiceDisponibilite.java', 'w', encoding='utf-8') as f:
    f.write(content)

print('✓ Fixed all encoding issues in ServiceDisponibilite.java')
