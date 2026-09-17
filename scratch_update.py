import re

file_path = r'src\frontend\MainFrame.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Using regex to find the elements flexibly
content = re.sub(
    r'(\{\s*"assets"\s*,\s*"[^"]+"\s*\})',
    r'\1,\n        { "sql", "   SQL Query" }',
    content
)

content = re.sub(
    r'(contentPanel\.add\(new AssetPanel\(\),\s*"assets"\);)',
    r'\1\n        contentPanel.add(new SQLQueryPanel(), "sql");',
    content
)

content = re.sub(
    r'(case\s*"assets"\s*->\s*"Assets";)',
    r'\1\n            case "sql"       -> "SQL Query";',
    content
)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainFrame updated successfully.")
