import sys
from PIL import Image

img = Image.open('Assets/sky_fury_ironblock.png')
img = img.convert('RGBA')
pixels = img.load()
width, height = img.size

def is_empty_row(y):
    for x in range(width):
        r, g, b, a = pixels[x, y]
        if a > 0 and (r < 240 or g < 240 or b < 240):
            return False
    return True

def is_empty_col(x, top, bottom):
    for y in range(top, bottom):
        r, g, b, a = pixels[x, y]
        if a > 0 and (r < 240 or g < 240 or b < 240):
            return False
    return True

# Find rows
row_bounds = []
in_row = False
row_top = 0

for y in range(height):
    empty = is_empty_row(y)
    if not empty and not in_row:
        in_row = True
        row_top = y
    elif empty and in_row:
        in_row = False
        if y - row_top > 20: # ignore noise
            row_bounds.append((row_top, y))

boxes = []
for top, bottom in row_bounds:
    # Find columns in this row
    in_col = False
    col_left = 0
    row_boxes = []
    for x in range(width):
        empty = is_empty_col(x, top, bottom)
        if not empty and not in_col:
            in_col = True
            col_left = x
        elif empty and in_col:
            in_col = False
            if x - col_left > 10:
                row_boxes.append([col_left, top, x - col_left, bottom - top])
    if in_col and width - col_left > 10:
        row_boxes.append([col_left, top, width - col_left, bottom - top])
    if row_boxes:
        boxes.append(row_boxes)

print(f"Found {len(boxes)} rows.")
for i, r in enumerate(boxes):
    print(f"Row {i+1}: {len(r)} glyphs")
    print(r)

