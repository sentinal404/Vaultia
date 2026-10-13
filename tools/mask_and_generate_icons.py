import math
import os
from PIL import Image

def process_logo():
    logo_path = 'app/src/main/res/drawable/vaultia_logo.png'
    src = Image.open(logo_path).convert('RGBA')
    w, h = src.size
    cx, cy = w / 2.0, h / 2.0
    r_inner = 500.0
    r_outer = 506.0

    pixels = src.load()
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            if dist >= r_outer:
                pixels[x, y] = (0, 0, 0, 0)
            elif dist > r_inner:
                factor = (r_outer - dist) / (r_outer - r_inner)
                r, g, b, a = pixels[x, y]
                pixels[x, y] = (r, g, b, int(a * factor))

    src.save(logo_path, format='PNG')
    print(f"Saved circular masked logo to {logo_path}")

    # Generate mipmap icons
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, size in sizes.items():
        dest_dir = os.path.join('app/src/main/res', folder)
        os.makedirs(dest_dir, exist_ok=True)
        resized = src.resize((size, size), Image.Resampling.LANCZOS)
        
        launcher_path = os.path.join(dest_dir, 'ic_launcher.png')
        launcher_round_path = os.path.join(dest_dir, 'ic_launcher_round.png')
        
        resized.save(launcher_path, format='PNG')
        resized.save(launcher_round_path, format='PNG')
        print(f"Generated {size}x{size} for {folder}")

if __name__ == '__main__':
    process_logo()
