# Any Converter

A lightweight Windows JavaFX batch converter with two options: **JFIF → JPG** and **WEBP → JPG**.

## What it does

Click one of the two buttons to scan `D:\\downloadz` for files of that type (directly inside the folder; no recursion). Both buttons share a progress bar and a status line that reports converted, skipped and failed files.

- **JFIF → JPG**: makes a byte-for-byte copy of every `.jfif` / `.JFIF` file with the same basename and a `.jpg` extension. JFIF is already JPEG, so this conversion is lossless.
- **WEBP → JPG**: decodes each `.webp` / `.WEBP` image and encodes it as a JPEG with the same basename. JPEG does not support transparency, so transparent pixels are placed on a **white background**. WebP → JPG is lossy, as JPEG re-encodes the image. Designed for still WebP images.

The original files are kept. The application **never overwrites existing `.jpg` files** and reports invalid images as failures. Failed conversions do not leave incomplete JPGs.

## Download and run on Windows

Download the latest built [AnyConverter.jar](dist/AnyConverter.jar) from this repository. The [Windows build workflow](../../actions/workflows/build.yml) publishes an updated JAR after each commit.

**Requirements:** Windows x64 and Java 21 or newer.

Double-click `AnyConverter.jar` if Java is associated with JAR files or run:

```powershell
java -jar AnyConverter.jar
```

The executable JAR bundles JavaFX's Windows runtime and the WebP decoder: there is no separate JavaFX or WebP codec installation.

## Build from source

```powershell
mvn clean verify
java -jar target/AnyConverter.jar
```

Build on **Windows x64** for Windows JavaFX native libraries. GitHub Actions runs tests and validates the shaded WebP decoder, then uploads and publishes the runnable JAR.

Main class: `hu.hazazs.anyconverter.Launcher`.

## UI

- Window title: **Any Converter**
- Buttons: **JFIF → JPG**, **WEBP → JPG**
- Progress bar immediately beneath the buttons
- Status text for progress and completion
- Black circular-arrows application icon on a transparent background
