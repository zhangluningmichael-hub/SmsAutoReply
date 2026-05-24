#!/usr/bin/env python3
"""Generate Android launcher icons at various densities from SVG + adaptive icon XML."""
import subprocess, os, struct, zlib

base = r"E:\SmsAutoReply\app\src\main\res"
svg_path = r"E:\SmsAutoReply\icon.svg"

# Android icon sizes
sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Try to use Python's built-in approach to create simple PNGs
# Since SVG rendering is complex without cairo, let's generate a solid color PNG
# with a simple text-based approach using raw PNG

def create_simple_png(width, height, r, g, b):
    """Create a simple solid color PNG with the given RGB values."""
    # PNG signature
    signature = b'\x89PNG\r\n\x1a\n'
    
    # IHDR chunk
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr = make_chunk(b'IHDR', ihdr_data)
    
    # IDAT chunk - raw image data
    raw_data = b''
    for y in range(height):
        raw_data += b'\x00'  # filter byte
        for x in range(width):
            # Check if pixel is inside rounded rect area
            # Simple rounded rect approximation
            rx, ry = 12 * width / 48, 12 * height / 48  # corner radius scaled
            corner = False
            px, py = x, y
            if px < rx and py < ry:
                # top-left
                corner = (rx - px) ** 2 + (ry - py) ** 2 > rx ** 2
            elif px > width - rx and py < ry:
                # top-right
                corner = (px - (width - rx)) ** 2 + (ry - py) ** 2 > rx ** 2
            elif px < rx and py > height - ry:
                # bottom-left
                corner = (rx - px) ** 2 + (py - (height - ry)) ** 2 > rx ** 2
            elif px > width - rx and py > height - ry:
                # bottom-right
                corner = (px - (width - rx)) ** 2 + (py - (height - ry)) ** 2 > rx ** 2
            
            if corner:
                raw_data += bytes([255, 255, 255, 255])
            else:
                raw_data += bytes([r, g, b])
    
    compressed = zlib.compress(raw_data)
    idat = make_chunk(b'IDAT', compressed)
    
    # IEND chunk
    iend = make_chunk(b'IEND', b'')
    
    return signature + ihdr + idat + iend

def create_icon_png(width, height):
    """Create a proper icon with chat bubble design."""
    import math
    signature = b'\x89PNG\r\n\x1a\n'
    
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr = make_chunk(b'IHDR', ihdr_data)
    
    bg_color = (25, 118, 210)
    white = (255, 255, 255)
    red = (255, 87, 34)
    
    raw_data = b''
    radius = int(width * 0.22)
    inset = int(width * 0.08)
    
    for y in range(height):
        raw_data += b'\x00'
        for x in range(width):
            # Background rounded rectangle
            px, py = x, y
            w, h = width, height
            r = radius
            
            # Check if outside rounded rect
            outside = False
            if px < r and py < r:
                outside = (r - px) ** 2 + (r - py) ** 2 > r ** 2
            elif px > w - r - 1 and py < r:
                outside = (px - (w - r - 1)) ** 2 + (r - py) ** 2 > r ** 2
            elif px < r and py > h - r - 1:
                outside = (r - px) ** 2 + (py - (h - r - 1)) ** 2 > r ** 2
            elif px > w - r - 1 and py > h - r - 1:
                outside = (px - (w - r - 1)) ** 2 + (py - (h - r - 1)) ** 2 > r ** 2
            
            if outside:
                raw_data += bytes([255, 255, 255])  # transparent-ish white
            else:
                # Chat bubble area
                bx = int(width * 0.28)
                by = int(height * 0.33)
                bw = int(width * 0.44)
                bh = int(height * 0.26)
                
                if bx <= x <= bx + bw and by <= y <= by + bh:
                    # Check badge circle
                    cx, cy = int(width * 0.67), int(height * 0.48)
                    cr = int(width * 0.13)
                    dx, dy = x - cx, y - cy
                    if dx * dx + dy * dy <= cr * cr:
                        raw_data += bytes(red)
                    elif x == bx or x == bx + bw or y == by or y == by + bh:
                        raw_data += bytes((200, 200, 200))
                    else:
                        raw_data += bytes(white)
                else:
                    raw_data += bytes(bg_color)
    
    compressed = zlib.compress(raw_data)
    idat = make_chunk(b'IDAT', compressed)
    iend = make_chunk(b'IEND', b'')
    
    return signature + ihdr + idat + iend

def make_chunk(chunk_type, data):
    chunk = chunk_type + data
    return struct.pack('>I', len(data)) + chunk + struct.pack('>I', zlib.crc32(chunk) & 0xffffffff)

# Generate icons for each density
for folder, size in sizes.items():
    out_dir = os.path.join(base, folder)
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "ic_launcher.png")
    
    png_data = create_icon_png(size, size)
    with open(out_path, 'wb') as f:
        f.write(png_data)
    
    # Also create ic_launcher_round.png (same icon for now)
    round_path = os.path.join(out_dir, "ic_launcher_round.png")
    with open(round_path, 'wb') as f:
        f.write(png_data)
    
    print(f"Created {out_path} ({size}x{size})")

# Create adaptive icon XML for v26+
adaptive_dir = os.path.join(base, "mipmap-anydpi-v26")
os.makedirs(adaptive_dir, exist_ok=True)

# ic_launcher.xml
with open(os.path.join(adaptive_dir, "ic_launcher.xml"), 'w') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n')
    f.write('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n')
    f.write('    <background android:drawable="@color/ic_launcher_background"/>\n')
    f.write('    <foreground android:drawable="@color/ic_launcher_foreground"/>\n')
    f.write('</adaptive-icon>\n')

# ic_launcher_round.xml
with open(os.path.join(adaptive_dir, "ic_launcher_round.xml"), 'w') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n')
    f.write('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n')
    f.write('    <background android:drawable="@color/ic_launcher_background"/>\n')
    f.write('    <foreground android:drawable="@color/ic_launcher_foreground"/>\n')
    f.write('</adaptive-icon>\n')

# Add colors resource needed by adaptive icon
colors_path = os.path.join(base, "values", "colors.xml")
with open(colors_path, 'r') as f:
    colors_content = f.read()

if 'ic_launcher_background' not in colors_content:
    colors_content = colors_content.replace('</resources>', 
        '    <color name="ic_launcher_background">#1976D2</color>\n'
        '    <color name="ic_launcher_foreground">#FFFFFF</color>\n'
        '</resources>')
    with open(colors_path, 'w') as f:
        f.write(colors_content)

print("Done! All icons generated.")
