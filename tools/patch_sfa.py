#!/usr/bin/env python3
import sys, os, re, shutil, json
from pathlib import Path

if len(sys.argv) < 3:
    print("Usage: patch_sfa.py <sfa_dir> <panel_url>")
    sys.exit(1)

root = Path(sys.argv[1]).resolve()
panel_url = sys.argv[2].rstrip("/")
builder_root = Path.cwd()

def read(p):
    return p.read_text(encoding="utf-8", errors="ignore")

def write(p, s):
    p.write_text(s, encoding="utf-8")

def replace_in_file(path, replacements):
    try:
        s = read(path)
    except Exception:
        return False
    orig = s
    for old, new in replacements:
        s = s.replace(old, new)
    if s != orig:
        write(path, s)
        return True
    return False

print(f"Patching SFA at: {root}")
print(f"Panel URL: {panel_url}")

# 1) App visible name strings: replace common labels with Nexora VPN.
for strings in root.rglob("strings.xml"):
    try:
        s = read(strings)
    except Exception:
        continue
    orig = s
    # Replace known app_name entries if present.
    s = re.sub(r'(<string\s+name="app_name"[^>]*>)(.*?)(</string>)', r'\1Nexora VPN\3', s)
    s = re.sub(r'(<string\s+name="application_name"[^>]*>)(.*?)(</string>)', r'\1Nexora VPN\3', s)
    s = s.replace(">sing-box<", ">Nexora VPN<")
    s = s.replace(">SFA<", ">Nexora VPN<")
    if s != orig:
        write(strings, s)
        print("Patched strings:", strings)

# 2) Add a Nexora panel descriptor asset. This does not break SFA, and lets us inspect the APK.
assets_dirs = []
for app_like in [root / "app/src/main/assets", root / "app/src/oss/assets", root / "app/src/foss/assets"]:
    app_like.mkdir(parents=True, exist_ok=True)
    assets_dirs.append(app_like)

descriptor = {
    "brand": "Nexora VPN",
    "panel_url": panel_url,
    "configs_api": f"{panel_url}/api/configs",
    "note": "Nexora builder patch. Real VPN engine remains official SFA/sing-box."
}
for ad in assets_dirs:
    (ad / "nexora-panel.json").write_text(json.dumps(descriptor, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Wrote asset:", ad / "nexora-panel.json")

# 3) Copy logo into drawable-nodpi folders. Android launcher icons may be adaptive/vector;
# this at least includes the brand logo resource without forcing fragile icon changes.
logo_src = builder_root / "assets/nexora-logo.jpg"
if logo_src.exists():
    for res in root.rglob("src/main/res"):
        nodpi = res / "drawable-nodpi"
        nodpi.mkdir(parents=True, exist_ok=True)
        shutil.copy(logo_src, nodpi / "nexora_logo.jpg")
        print("Copied logo:", nodpi / "nexora_logo.jpg")

# 4) Try package/application label branding in AndroidManifest if present.
for manifest in root.rglob("AndroidManifest.xml"):
    try:
        s = read(manifest)
    except Exception:
        continue
    orig = s
    # If label hard-coded or resource exists, keep resource. Avoid breaking manifest.
    s = s.replace('android:label="sing-box"', 'android:label="Nexora VPN"')
    s = s.replace('android:label="SFA"', 'android:label="Nexora VPN"')
    if s != orig:
        write(manifest, s)
        print("Patched manifest:", manifest)

# 5) Create a small README inside cloned repo for traceability.
(root / "NEXORA_PATCH_APPLIED.txt").write_text(
    f"Nexora VPN patch applied.\nPanel: {panel_url}\nConfigs API: {panel_url}/api/configs\n",
    encoding="utf-8"
)

print("Patch completed.")
