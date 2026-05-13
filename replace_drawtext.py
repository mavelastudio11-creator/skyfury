import re

filepath = "/Users/abbypangeran/Library/CloudStorage/OneDrive-Personal/Apps/Sky Fury/app/src/main/java/com/mavelastudio/skyfury/SkyFuryGameView.java"
with open(filepath, "r") as f:
    content = f.read()

# Replace all canvas.drawText(arg1, arg2, arg3, textPaint);
# with drawIronblockText(canvas, arg1, arg2, arg3, textPaint);
# But we need to ensure we don't mess up multi-line if they exist.
# Let's use a regex that matches on a single line.

def replace_func(match):
    arg1 = match.group(1)
    arg2 = match.group(2)
    arg3 = match.group(3)
    return f"drawIronblockText(canvas, {arg1}, {arg2}, {arg3}, textPaint);"

# The regex captures three arguments before textPaint
# arg1 could have commas inside parentheses (like method calls), so we need to be careful.
# Actually, since all arguments are simple or just one level of parens, a simpler regex works:
# We just look at lines.
lines = content.split('\n')
for i, line in enumerate(lines):
    if "canvas.drawText(" in line and "textPaint);" in line:
        if "drawButton" in line: # skip if it's the signature definition, but it's not.
            pass
        # Extract everything inside canvas.drawText( ... , textPaint)
        m = re.search(r'canvas\.drawText\((.*),\s*textPaint\);', line)
        if m:
            args = m.group(1)
            # Replace canvas.drawText( ... , textPaint) with drawIronblockText(canvas, ... , textPaint)
            lines[i] = line.replace(f"canvas.drawText({args}, textPaint);", f"drawIronblockText(canvas, {args}, textPaint);")

with open(filepath, "w") as f:
    f.write('\n'.join(lines))

print("Replacements done.")
