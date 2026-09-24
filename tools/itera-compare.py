"""Compare paired synthetic gallery captures, excluding Android system bars."""
from pathlib import Path
from PIL import Image, ImageChops
root = Path(r"D:\Projects\Itera\captures")
for theme in ("false", "true"):
    compared, differences = 0, []
    for reference in sorted(root.glob(f"com.itera.app-*-{theme}-*.png")):
        actual = root / reference.name.replace("com.itera.app-", "com.wivernz.itera-")
        if not actual.exists():
            continue
        with Image.open(reference) as a, Image.open(actual) as b:
            bounds = (0, 80, a.width, a.height - 70)
            box = ImageChops.difference(a.convert("RGB").crop(bounds), b.convert("RGB").crop(bounds)).getbbox()
        compared += 1
        if box:
            differences.append((reference.name.removeprefix("com.itera.app-"), box))
    print(f"{theme=}: {compared} pairs, {compared - len(differences)} exact app-area matches")
    for name, box in differences:
        print(f"  {name}: {box}")
