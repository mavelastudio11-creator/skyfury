import cv2
import numpy as np

# Load image
img = cv2.imread('Assets/sky_fury_ironblock.png')
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

# Threshold to find non-white regions (the letters and drop shadows)
# The background is white (255)
_, thresh = cv2.threshold(gray, 240, 255, cv2.THRESH_BINARY_INV)

# Find contours
contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

boxes = []
for c in contours:
    x, y, w, h = cv2.boundingRect(c)
    if w > 10 and h > 10: # Filter noise
        boxes.append([x, y, w, h])

# Sort boxes top-to-bottom, then left-to-right
# Group into rows based on Y coordinate
boxes.sort(key=lambda b: b[1])

rows = []
current_row = []
current_y = -1
for b in boxes:
    if current_y == -1:
        current_y = b[1]
    
    # If the Y difference is less than half the height of a typical letter, it's the same row
    if abs(b[1] - current_y) < 50:
        current_row.append(b)
        # update current_y to average
        current_y = sum(br[1] for br in current_row) / len(current_row)
    else:
        # Sort current row by X
        current_row.sort(key=lambda x: x[0])
        rows.append(current_row)
        current_row = [b]
        current_y = b[1]

if current_row:
    current_row.sort(key=lambda x: x[0])
    rows.append(current_row)

print(f"Found {len(rows)} rows.")
for i, r in enumerate(rows):
    print(f"Row {i+1}: {len(r)} glyphs")
