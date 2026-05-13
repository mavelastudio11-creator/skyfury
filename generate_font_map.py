import sys
import os
from PIL import Image, ImageChops

img = Image.open('Assets/sky_fury_ironblock.png').convert('RGBA')
width, height = img.size
pixels = img.load()

def get_row_profile():
    profile = []
    for y in range(height):
        # Count non-white/transparent pixels
        count = 0
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0 and (r < 240 or g < 240 or b < 240):
                count += 1
        profile.append(count)
    return profile

def get_col_profile(top, bottom):
    profile = []
    for x in range(width):
        count = 0
        for y in range(top, bottom):
            r, g, b, a = pixels[x, y]
            if a > 0 and (r < 240 or g < 240 or b < 240):
                count += 1
        profile.append(count)
    return profile

# Find rows by finding regions where row profile > 0
row_profile = get_row_profile()
rows = []
in_row = False
start_y = 0
for y, count in enumerate(row_profile):
    if count > 5 and not in_row:
        in_row = True
        start_y = y
    elif count <= 5 and in_row:
        in_row = False
        if y - start_y > 20:
            rows.append((start_y, y))
if in_row and height - start_y > 20:
    rows.append((start_y, height))

all_boxes = []
for r_idx, (top, bottom) in enumerate(rows):
    col_profile = get_col_profile(top, bottom)
    cols = []
    in_col = False
    start_x = 0
    for x, count in enumerate(col_profile):
        if count > 0 and not in_col:
            in_col = True
            start_x = x
        elif count == 0 and in_col:
            in_col = False
            if x - start_x > 10:
                cols.append((start_x, x))
    if in_col and width - start_x > 10:
        cols.append((start_x, width))
    
    row_boxes = []
    for (left, right) in cols:
        row_boxes.append((left, top, right - left, bottom - top))
    all_boxes.append(row_boxes)

print(f"Detected {len(all_boxes)} rows.")
for i, boxes in enumerate(all_boxes):
    print(f"Row {i+1}: {len(boxes)} glyphs")

# Let's map them to expected characters based on my visual layout.
# Uppercase generation:
# Prompt 2: All 26 letters A-Z, 0-9, ! ? + - /
# The uppercase image has:
# Row 1: A B C D E F G H (8 letters)
# Row 2: I J K L M N O (7 letters)
# Row 3: P Q R S T U V (7 letters)
# Row 4: W X Y Z (4 letters)
# Row 5: 0 1 2 3 4 5 6 7 8 9 (10 letters)
# Row 6: ! ? + - / (5 letters)
# Total uppercase rows: 6.
# Lowercase generation (fixed):
# Row 7: a b c d e f g (7 letters)
# Row 8: h i j k l m n (7 letters)
# Row 9: o p q r s t u (7 letters)
# Row 10: v w x y z (5 letters)

# Let's see if the detected rows match this.
