import re

with open("d:\\Micro Project\\frontend\\src\\pages\\LandingPage.css", "r", encoding="utf-8") as f:
    css = f.read()

# Replace all theme(...) with a placeholder
css = re.sub(r"theme\('colors\.[^']+'\)", "#111c2d", css)

with open("d:\\Micro Project\\frontend\\src\\pages\\LandingPage.css", "w", encoding="utf-8") as f:
    f.write(css)
