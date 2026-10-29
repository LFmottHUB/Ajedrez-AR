# AR Chess Android 0.1

Primera adaptación Android de AR-Chess para probar un único marcador selector. **APK de prueba: cámara, rendimiento y controles AR pendientes de validación en un teléfono real.** Compilar no equivale a haber validado el seguimiento.

## Instalar y probar

1. Copia `AR-Chess-0.1.0-prueba.apk` al teléfono y ábrelo. Android puede solicitar habilitar la instalación desde la aplicación que abre el archivo.
2. Abre **AR Chess Prueba** y concede permiso de cámara. No requiere cuenta, servidor ni permiso de Internet; no graba ni envía video.
3. Imprime la página del marcador de 10 cm del Word entregado, en papel Carta, escala 100 %, blanco y negro, calidad normal/alta. Comprueba el lado exterior negro con una regla. Mantén 1 cm o más de margen blanco.
4. Sostén el teléfono horizontalmente y enfoca UN marcador completo. El indicador debe cambiar a «Marcador detectado» y aparecerá el tablero.
5. Toca la base de una pieza y luego una casilla verde. Si promociona un peón, elige la nueva pieza.
6. Prueba ocultar el marcador, volver a mostrarlo, cambiar de aplicación y regresar. La partida se guarda; no se debe iniciar una partida nueva por perder el marcador.

**No se promete reconocer cualquier tamaño/distancia.** El marcador tiene que estar nítido, completo, con contraste y suficientes píxeles. Los tamaños 5/10/15 cm son opciones de ensayo. La escala del tablero es relativa al lado del patrón; no mide centímetros reales. No hace falta un segundo marcador.

## Funciones de esta entrega

- Cámara trasera y seguimiento del patrón `selector.patt` original mediante artoolkitX 1.1.22.
- Un único patrón, blanco y negro, umbral automático Otsu y filtro de pose.
- Tablero 3D y piezas geométricas nuevas, adaptadas al móvil; no son los modelos `.ive` originales.
- Dos personas en el mismo teléfono; movimientos legales, enroque, captura al paso y cuatro promociones.
- Jaque, jaque mate, ahogado, detección conservadora de material insuficiente, tablas automáticas por 75 jugadas o cinco repeticiones. Reclamación de tablas por 50 jugadas o tres repeticiones en la posición actual.
- Rival básico local con búsqueda alfa-beta de profundidad 1, 2 o 3. **No es Stockfish ni tiene una fuerza de juego certificada.**
- Atrás/Adelante, partida guardada localmente, ajuste de escala y modo Solo tablero para probar el reconocimiento.
- Al perder el marcador se oculta el tablero; no se fija artificialmente a una posición de la pantalla.

La app inicia con una partida de dos personas. **Nueva** permite seleccionar rival básico. Después de revisar el historial, **Rival juega** reanuda el turno del ordenador si se está en el final del historial. El ordenador juega negras.

## Compatibilidad prevista

Android 7.0 o posterior (API 24), CPU ARM de 32 o 64 bits, OpenGL ES 2.0 y cámara trasera. Target SDK 34, compile SDK 35. No se ha validado un modelo concreto porque aún no se ha informado el teléfono.

Las bibliotecas precompiladas artoolkitX 1.1.22 tienen segmentos ELF alineados a 4 KB. **Esta entrega no certifica compatibilidad con sistemas Android de páginas de 16 KB.** Algunos podrían usar compatibilidad del sistema; otros requerirán recompilar el SDK con una cadena moderna. Tampoco es una publicación de Google Play.

Se utiliza la calibración/fallback que proporcione el SDK. Una calibración inadecuada puede afectar la alineación 3D. Se debe comprobar visualmente en el dispositivo. El APK no declara permiso de Internet.

## Abrir y modificar

Abre esta carpeta en Android Studio o VS Code. Para compilar instala JDK 17, Android SDK Platform 35 y Build Tools 35.0.0. Android Studio puede gestionar el SDK. La primera compilación Gradle descarga sus dependencias.

En Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

En Linux/macOS:

```sh
./gradlew :app:assembleDebug
```

El resultado Gradle estará en `app/build/outputs/apk/debug/app-debug.apk`. La firma de prueba local puede ser distinta de la del APK entregado: en ese caso desinstala la versión anterior antes de instalar la nueva; eso borra la partida guardada.

También se incluye `tools/build-local.sh`, utilizado para crear el APK entregado sin descargar bibliotecas Maven: necesita Linux, Python 3, JDK 17, SDK 35 y las variables `JAVA_HOME` y `ANDROID_HOME`. Solo usa herramientas oficiales del SDK. Su clave de prueba se crea en `build/local/test.keystore`, no se incluye en este ZIP y no debe utilizarse para producción.

La compilación Gradle no pudo completarse en este entorno porque no se resolvió el plugin Android desde los repositorios remotos. La ruta de compilación directa con el SDK sí se ejecutó y generó el APK entregado. El wrapper Gradle y su configuración se incluyen para Android Studio, pero esa ruta todavía requiere verificación en tu PC.

### Dónde modificar cada parte

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.java` | Controles, permisos, guardado y ciclo de vida |
| `ChessRenderer.java` | Cámara AR, patrón, matrices, selección táctil y escala |
| `Mesh.java` | Geometría de tablero y piezas |
| `Chess.java` | Reglas y posiciones FEN |
| `Game.java` | Historial y estado de partida |
| `BasicEngine.java` | Rival básico, sustituible por un adaptador UCI |
| `app/src/main/assets/Data/selector.patt` | Patrón que se reconoce |

Las clases Java de la aplicación están en `app/src/main/java/com/example/archess`. Los bindings originales sin modificar se encuentran en `org/artoolkitx/arx/arxj`. Las bibliotecas ARM están en `app/src/main/jniLibs`.

## Verificaciones realizadas

- Compilación Java y dex, empaquetado APK, verificación de firmas APK v2/v3 y alineación ZIP: correctas.
- 20 comprobaciones automáticas del juego: correctas. Incluyen perft inicial 20, 400, 8902, 197281; Kiwipete 48, 2039, 97862; posición de en passant, promoción, mate, historial, repetición y jugada legal del rival.
- El archivo del patrón coincide byte a byte con la revisión original del repositorio.
- Word: cuatro páginas renderizadas y revisadas; dimensiones OOXML exactas de las imágenes 5 × 5, 10 × 10 y 15 × 15 cm. Esa medida requiere imprimir a tamaño real.
- **No ejecutado:** instalación en un Android físico/emulador, captura real de cámara, reconocimiento nativo del PDF, tasa de cuadros, estabilidad, precisión táctil y reanudación en dispositivo. El siguiente hito es tu prueba real.

Ejecutar las pruebas sin Android:

```sh
mkdir -p build/tests
javac -encoding UTF-8 -d build/tests app/src/main/java/com/example/archess/Chess.java app/src/main/java/com/example/archess/Game.java app/src/main/java/com/example/archess/BasicEngine.java tests/ChessTest.java
java -cp build/tests com.example.archess.ChessTest
```

## Resultado que necesitamos de tu prueba

Modelo y versión Android; si instala/abre; tamaño impreso; distancia aproximada; iluminación; si el marcador se detecta, si el tablero tiembla, si tocar una casilla selecciona la correcta y si se conserva la partida al volver a abrir. Si hay un fallo, indica el mensaje exacto o aporta una captura.

## Trabajo que queda después de la prueba

1. Ajustar el seguimiento según resultados reales de tamaños y ángulos.
2. Resolver compatibilidad de páginas de 16 KB si el teléfono la requiere.
3. Integrar Stockfish para Android y su código/licencia GPL correspondiente; en esta entrega lo sustituye temporalmente el rival básico.
4. Mejorar modelos y animaciones. Los relojes, ordenador contra ordenador y equivalencia completa con la interfaz de escritorio no están implementados.
5. Preparar firma de distribución y validación de dispositivos, si se desea publicar.

## Origen y licencias

- Proyecto de referencia: https://github.com/carlosmccosta/AR-Chess
- Revisión fijada: `bf5e5cca89ea62ea46ba3e6381a15b86d88a7a90`.
- Se reutiliza el patrón selector con su licencia MIT; la interfaz de Windows/osgART y el ejecutable de Stockfish para Windows no se incluyen.
- SDK Android oficial: https://github.com/artoolkitx/artoolkitx/releases/tag/1.1.22
- Fuente de artoolkitX correspondiente: https://github.com/artoolkitx/artoolkitx/tree/1.1.22
- Archivo de fuentes: https://github.com/artoolkitx/artoolkitx/archive/refs/tags/1.1.22.zip
- Licencias completas incluidas en `app/src/main/assets/licenses`. Los bindings y bibliotecas artoolkitX permanecen sin modificar. Para reemplazar el SDK, recompila su fuente oficial y sustituye los bindings/bibliotecas; no se bloquea esa sustitución.

Esta es una reimplementación móvil gradual basada en el proyecto, no una recompilación del ejecutable Windows. La revisión inicial aprobada prioriza comprobar el seguimiento en el teléfono antes de declarar la migración terminada.
