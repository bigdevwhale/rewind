"""Writes the small hand-made resources: themes, colours, launcher icon and notification icon.

python tools/res.py
"""
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res"

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Rewind" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="colorPrimary">@color/neon</item>
        <item name="colorAccent">@color/neon</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FF0D0B14</color>
    <color name="neon">#FFFF4FA3</color>
</resources>
""",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_monochrome" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- The store at night: deep violet, and the blue carpet at the bottom. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0D0B14" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#1C2350" android:pathData="M0,78h108v30h-108z" />
    <path android:fillColor="#161A3C" android:pathData="M0,74h108v4h-108z" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": """<?xml version="1.0" encoding="utf-8"?>
<!-- A black VHS tape with two reels, outlined in pink neon, and a cyan rewind mark. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:strokeColor="#55FF4FA3" android:strokeWidth="6" android:fillColor="#00000000"
        android:pathData="M30,38h48a3,3 0,0 1,3 3v26a3,3 0,0 1,-3 3h-48a3,3 0,0 1,-3 -3v-26a3,3 0,0 1,3 -3z" />
    <path android:fillColor="#17131F" android:strokeColor="#FF4FA3" android:strokeWidth="2"
        android:pathData="M30,38h48a3,3 0,0 1,3 3v26a3,3 0,0 1,-3 3h-48a3,3 0,0 1,-3 -3v-26a3,3 0,0 1,3 -3z" />
    <path android:fillColor="#0D0B14" android:pathData="M38,48h32a6,6 0,0 1,0 12h-32a6,6 0,0 1,0 -12z" />
    <path android:fillColor="#4A2E1E" android:pathData="M34,54a8,8 0,1 1,16 0a8,8 0,1 1,-16 0z" />
    <path android:fillColor="#F4EEE3" android:pathData="M38.5,54a3.5,3.5 0,1 1,7 0a3.5,3.5 0,1 1,-7 0z" />
    <path android:fillColor="#F4EEE3" android:pathData="M62.5,54a3.5,3.5 0,1 1,7 0a3.5,3.5 0,1 1,-7 0z" />
    <path android:fillColor="#F5F0E4" android:pathData="M34,41h40v4h-40z" />
    <path android:fillColor="#52E6FF" android:pathData="M54,62l-6,3.5l6,3.5z M60,62l-6,3.5l6,3.5z" />
</vector>
""",
    "drawable/ic_monochrome.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFF" android:fillType="evenOdd"
        android:pathData="M30,38h48a3,3 0,0 1,3 3v26a3,3 0,0 1,-3 3h-48a3,3 0,0 1,-3 -3v-26a3,3 0,0 1,3 -3z M38,48h32a6,6 0,0 1,0 12h-32a6,6 0,0 1,0 -12z" />
    <path android:fillColor="#FFFFFF" android:pathData="M38.5,54a3.5,3.5 0,1 1,7 0a3.5,3.5 0,1 1,-7 0z M62.5,54a3.5,3.5 0,1 1,7 0a3.5,3.5 0,1 1,-7 0z" />
</vector>
""",
    "drawable/ic_notification.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFF" android:fillType="evenOdd"
        android:pathData="M3,6h18a1,1 0,0 1,1 1v10a1,1 0,0 1,-1 1h-18a1,1 0,0 1,-1 -1v-10a1,1 0,0 1,1 -1z M7,10h10a2,2 0,0 1,0 4h-10a2,2 0,0 1,0 -4z" />
</vector>
""",
}


def main():
    for name, text in FILES.items():
        f = RES / name
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
