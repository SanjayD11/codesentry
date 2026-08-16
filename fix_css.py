import re

with open("frontend/src/pages/LandingPage.css", "r") as f:
    css = f.read()

css = css.replace("theme('colors.background')", "#f9f9ff")
css = css.replace("theme('colors.on-background')", "#111c2d")
css = css.replace("theme('colors.outline-variant')", "#c2c6d6")
css = css.replace("theme('colors.surface-container-lowest')", "#ffffff")
css = css.replace("theme('colors.primary')", "#0058be")
css = css.replace("theme('colors.tertiary')", "#006947")

with open("frontend/src/pages/LandingPage.css", "w") as f:
    f.write(css)
