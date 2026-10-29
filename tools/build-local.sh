#!/usr/bin/env bash
# Build a signed TEST APK using only JDK 17 and Android SDK 35 (no Maven).
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
BT="$ANDROID_HOME/build-tools/35.0.0"
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
OUT="$PWD/build/local"
mkdir -p "$OUT/classes" "$OUT/dex"
python3 - <<'PY'
import xml.etree.ElementTree as E
E.register_namespace('android','http://schemas.android.com/apk/res/android')
t=E.parse('app/src/main/AndroidManifest.xml')
t.getroot().set('package','com.example.archess')
t.write('build/local/AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
PY
"$BT/aapt" package -f -M "$OUT/AndroidManifest.xml" -I "$PLATFORM" -A app/src/main/assets -F "$OUT/resources.ap_"
find app/src/main/java -name '*.java' > "$OUT/sources.txt"
"$JAVA_HOME/bin/javac" -encoding UTF-8 --release 8 -classpath "$PLATFORM" -d "$OUT/classes" @"$OUT/sources.txt"
"$JAVA_HOME/bin/jar" cf "$OUT/classes.jar" -C "$OUT/classes" .
PATH="$JAVA_HOME/bin:$PATH" "$BT/d8" --lib "$PLATFORM" --min-api 24 --output "$OUT/dex" "$OUT/classes.jar"
python3 - <<'PY'
from pathlib import Path
import zipfile
p=Path('build/local')
with zipfile.ZipFile(p/'resources.ap_') as src,zipfile.ZipFile(p/'unsigned.apk','w') as z:
 for n in src.namelist():z.writestr(n,src.read(n),compress_type=zipfile.ZIP_DEFLATED)
 for f in (p/'dex').glob('*.dex'):z.write(f,f.name,compress_type=zipfile.ZIP_DEFLATED)
 for f in Path('app/src/main/jniLibs').rglob('*.so'):z.write(f,'lib/'+str(f.relative_to('app/src/main/jniLibs')),compress_type=zipfile.ZIP_DEFLATED)
PY
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [[ ! -f "$OUT/test.keystore" ]]; then
 "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$OUT/test.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=AR Chess Test' -keyalg RSA -keysize 2048 -validity 10000
fi
PATH="$JAVA_HOME/bin:$PATH" "$BT/apksigner" sign --v4-signing-enabled false --ks "$OUT/test.keystore" --ks-pass pass:android --key-pass pass:android --out "$OUT/AR-Chess-0.1.0-prueba.apk" "$OUT/aligned.apk"
PATH="$JAVA_HOME/bin:$PATH" "$BT/apksigner" verify --verbose "$OUT/AR-Chess-0.1.0-prueba.apk"
"$BT/zipalign" -c 4 "$OUT/AR-Chess-0.1.0-prueba.apk"
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/local/AR-Chess-0.1.0-prueba.apk') as z:
 assert z.testzip() is None
 assert 'AndroidManifest.xml' in z.namelist()
PY
echo "$OUT/AR-Chess-0.1.0-prueba.apk"
