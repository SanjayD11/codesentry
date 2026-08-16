import re

with open("frontend/src/pages/LandingPage.css", "r") as f:
    css = f.read()

# Replace all theme(...) with a placeholder
css = re.sub(r"theme\('colors\.[^']+'\)", "#111c2d", css)

with open("frontend/src/pages/LandingPage.css", "w") as f:
    f.write(css)
