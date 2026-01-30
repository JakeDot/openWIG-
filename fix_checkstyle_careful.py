#!/usr/bin/env python3
import os
import re

def fix_typecast_whitespace(content):
    """Add space after typecast: (Type)var -> (Type) var"""
    # Match (Type)identifier where Type can include generics
    # Only fix if followed by identifier character (not operator, whitespace, or semicolon)
    result = []
    i = 0
    while i < len(content):
        if content[i] == '(' and i + 1 < len(content):
            # Find matching close paren
            depth = 1
            j = i + 1
            while j < len(content) and depth > 0:
                if content[j] == '(': depth += 1
                elif content[j] == ')': depth -= 1
                j += 1
            
            # Check if this looks like a typecast
            if depth == 0 and j < len(content):
                # Check if followed by identifier without space
                if content[j].isalpha() or content[j] == '_':
                    cast_content = content[i+1:j-1]
                    # Simple heuristic: if it contains only valid type characters
                    if re.match(r'^[A-Za-z0-9_.<>, \[\]]+$', cast_content):
                        result.append(content[i:j])
                        result.append(' ')
                        i = j
                        continue
        
        result.append(content[i])
        i += 1
    
    return ''.join(result)

def fix_whitespace_around_operators(content):
    """Fix whitespace around +, -, *, /, etc. but NOT ++, --, or in strings"""
    lines = content.split('\n')
    result = []
    
    for line in lines:
        # Skip if line is a comment
        stripped = line.lstrip()
        if stripped.startswith('//') or stripped.startswith('*'):
            result.append(line)
            continue
        
        # Fix + and - (but not ++ or --)
        # Pattern: alphanumeric or ) followed by +/- followed by alphanumeric or (
        line = re.sub(r'(\w|\))(\+)(\w|\()', r'\1 + \3', line)
        line = re.sub(r'(\w|\))(\-)(\w|\()', r'\1 - \3', line)
        
        # Fix * and / (careful with comments)
        if '/*' not in line and '*/' not in line:
            line = re.sub(r'(\w)(\*)(\w)', r'\1 * \3', line)
        if '//' not in line:
            line = re.sub(r'(\w)(/)(\w)', r'\1 / \3', line)
        
        # Fix =, ==, !=, <=, >= 
        line = re.sub(r'(\w)(==)(\w)', r'\1 == \3', line)
        line = re.sub(r'(\w)(!=)(\w)', r'\1 != \3', line)
        line = re.sub(r'(\w)(<=)(\w)', r'\1 <= \3', line)
        line = re.sub(r'(\w)(>=)(\w)', r'\1 >= \3', line)
        
        result.append(line)
    
    return '\n'.join(result)

def process_file(filepath):
    """Process a single Java file."""
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    original = content
    
    # Apply fixes
    content = fix_typecast_whitespace(content)
    content = fix_whitespace_around_operators(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    src_dir = 'OpenWIGLibrary/src'
    count = 0
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if process_file(filepath):
                    count += 1
                    print(f"Fixed: {filepath}")
    print(f"\nProcessed {count} files")

if __name__ == '__main__':
    main()
