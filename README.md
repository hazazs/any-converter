# Any Converter

A small Windows JavaFX file converter. Currently supports **JFIF → JPG**.

## What it does

1. Start the application.
2. Click **JFIF → JPG**.
3. The app scans `D:\\downloadz` (the files immediately in that folder, not subfolders).
4. Every `.jfif` / `.JFIF` file that contains JPEG data is copied to the same folder with the same basename and a `.jpg` extension. Progress updates under the button.

JFIF is already JPEG, so copying the bytes is lossless and preserves the original image quality and metadata. The original `.jfif` files are **kept**. Existing `.jpg` files are **never overwritten**. Invalid JPEGs are counted as failures. When finished, the window reports converted, skipped, and failed counts.

## Download and run on Windows

Download the latest built [AnyConverter.jar](dist/AnyConverter.jar) after the [Windows build workflow](../../actions/workflows/build.yml) finishes.

**Requirements:** Windows x64 and Java 21 or newer installed.

Double-click `AnyConverter.jar` if Java is associated with `.jar` files, or launch it from the terminal:

```powershell
java -jar AnyConverter.jar
```

The Maven build includes JavaFX's Windows classes and native libraries inside the executable JAR; it does not require a separate JavaFX installation.

## Build from source

```powershell
mvn clean verify
java -jar target/AnyConverter.jar
```

Build on **Windows x64** to include Windows native JavaFX libraries. The GitHub Actions workflow runs the tests on Windows and uploads the runnable JAR as an artifact, then publishes it to `dist/AnyConverter.jar`.

Main class: `hu.hazazs.anyconverter.Launcher`.

## Current UI

- Title: **Any Converter**
- One button: **JFIF → JPG**
- Progress bar immediately beneath the button
- Small status line showing progress and completion results
- Black circular-arrows icon on a transparent background
