# 📖 Read-O-Meter Pro

> A modern JavaFX desktop application that analyses your writing in real time — measuring reading time, word count, complexity, and overused words.

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=flat-square&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-17%2B-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey?style=flat-square)

---

## 📋 Table of Contents

- [About the Project](#-about-the-project)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Installation & Running](#-installation--running)
  - [Method 1 — Run from Source (Quick Start)](#method-1--run-from-source-quick-start)
  - [Method 2 — Maven Build (Recommended)](#method-2--maven-build-recommended)
  - [Method 3 — Build a .exe Installer](#method-3--build-a-exe-installer)
- [How to Use](#-how-to-use)
- [Logic & Algorithms](#-logic--algorithms)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)

---

## 🧠 About the Project

**Read-O-Meter Pro** is a desktop productivity tool built with JavaFX that gives writers, students, and professionals instant feedback on their writing. Drop in a `.txt` file or type directly — the app analyses your text in the background without ever freezing the UI.

This project was built to demonstrate:
- Clean **MVC architecture** in a JavaFX application
- **Asynchronous task handling** using `javafx.concurrent.Task`
- Separation of UI styling using an **external CSS file** (dark mode)
- Modern **Java 17+ syntax** including records, `Set.of()`, and streams

---

## ✨ Features

| Feature | Description |
|---|---|
| ⏱ **Reading Time** | Estimates reading time at 260 WPM |
| 🔢 **Word Count** | Live word count as you type |
| 🎓 **Complexity Level** | Easy / Medium / Hard based on average word length |
| 🚩 **Tired Word Detector** | Flags overused filler words like *very*, *just*, *really* |
| 📂 **Drag & Drop** | Drop any `.txt` file onto the editor to load it instantly |
| 🎨 **Dark Mode UI** | Material-inspired dark theme with rounded cards and accent colours |
| ⚡ **Non-blocking UI** | All analysis and file I/O runs on background threads |
| 💡 **Tooltip Guide** | Hover over the editor to see drag-and-drop instructions |

---

## 📸 Screenshots

```
┌─────────────────────────────────────────────────────────┐
│  📖 Read-O-Meter Pro                                    │
│  Paste, type, or drop a .txt file to analyse your text  │
├──────────────────────────────────────┬──────────────────┤
│                                      │  📊 Analysis     │
│   TextArea (Drag & Drop enabled)     │ ┌──────────────┐ │
│                                      │ │ ⏱ Reading    │ │
│   Start typing your content here…    │ │   2 min 10s  │ │
│                                      │ ├──────────────┤ │
│                                      │ │ 🔢 Words     │ │
│                                      │ │   562        │ │
│                                      │ ├──────────────┤ │
│                                      │ │ 🎓 Complexity│ │
│                                      │ │   Medium     │ │
│                                      │ ├──────────────┤ │
│                                      │ │ 🚩 Tired     │ │
│                                      │ │ very(3),just │ │
│                                      │ └──────────────┘ │
├──────────────────────────────────────┴──────────────────┤
│  ✔  Analysis complete — 562 words found.                │
└─────────────────────────────────────────────────────────┘
```

---

## 🛠 Tech Stack

- **Language:** Java 17+
- **UI Framework:** JavaFX 17+
- **Styling:** JavaFX CSS (external `style.css`)
- **Concurrency:** `javafx.concurrent.Task`, `Platform.runLater()`
- **Build Tool:** Maven *(optional but recommended)*
- **Packaging:** `jpackage` (JDK built-in, Java 14+)

---

## 📁 Project Structure

```
ReadOMeterPro/
│
├── src/
│   ├── ReadOMeterApp.java      # Main application file (View + Controller)
│   └── style.css               # External dark-mode stylesheet
│
├── out/                        # Compiled .class files (generated)
├── dist/                       # Packaged JAR (generated)
├── installer/                  # Final .exe / .dmg / .deb (generated)
│
├── pom.xml                     # Maven build file (optional)
└── README.md                   # This file
```

---

## 🏗 Architecture

The project follows an **MVC-lite pattern** with two inner classes keeping responsibilities clean:

```
ReadOMeterApp.java
│
├── ReadOMeterApp          → JavaFX Application entry-point
│                            Wires AppView + TextController together
│
├── AppView                → VIEW layer
│                            Builds all JavaFX nodes (BorderPane, TextArea,
│                            Sidebar, StatCards, StatusBar).
│                            Owns all UI update methods.
│                            Has NO business logic.
│
└── TextController         → CONTROLLER / LOGIC layer
                             Pure Java — zero JavaFX imports.
                             Can be unit-tested independently.
                             Exposes a single analyse(String) method.
                             Returns an immutable AnalysisResult record.
```

**Async flow for drag-and-drop:**
```
User drops file
      │
      ▼
DragDropped handler (FX Thread)
      │  spawns
      ▼
Task<String>  ──── background thread ──── reads file bytes
      │
      │  on success → Platform.runLater()
      ▼
TextArea.setText()  (back on FX Thread — UI stays responsive)
      │
      ▼
Debounced listener → Task<AnalysisResult> (background thread)
      │
      ▼
updateStats()  (FX Thread — stat cards refresh)
```

---

## ✅ Prerequisites

Make sure you have the following installed before running the project:

| Tool | Version | Download |
|---|---|---|
| JDK (OpenJDK) | 17 or higher | [adoptium.net](https://adoptium.net) |
| JavaFX SDK | 17 or higher | [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/) |
| Maven *(optional)* | 3.8+ | [maven.apache.org](https://maven.apache.org) |
| WiX Toolset *(Windows .exe only)* | 3.x | [wixtoolset.org](https://wixtoolset.org) |

---

## 🚀 Installation & Running

### Method 1 — Run from Source (Quick Start)

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/read-o-meter-pro.git
cd read-o-meter-pro
```

**2. Compile**

> Replace `C:\javafx-sdk-21\lib` with the actual path to your JavaFX SDK's `lib` folder.

**Windows:**
```bash
javac --module-path "C:\javafx-sdk-21\lib" ^
      --add-modules javafx.controls ^
      -d out ^
      src\ReadOMeterApp.java

copy src\style.css out\
```

**macOS / Linux:**
```bash
javac --module-path "/opt/javafx-sdk-21/lib" \
      --add-modules javafx.controls \
      -d out \
      src/ReadOMeterApp.java

cp src/style.css out/
```

**3. Run**

**Windows:**
```bash
java --module-path "C:\javafx-sdk-21\lib" ^
     --add-modules javafx.controls ^
     -cp out ^
     ReadOMeterApp
```

**macOS / Linux:**
```bash
java --module-path "/opt/javafx-sdk-21/lib" \
     --add-modules javafx.controls \
     -cp out \
     ReadOMeterApp
```

---

### Method 2 — Maven Build (Recommended)

Maven automatically downloads JavaFX — no manual SDK setup needed.

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/read-o-meter-pro.git
cd read-o-meter-pro
```

**2. Add `pom.xml`** to the project root with this content:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.yourname</groupId>
  <artifactId>read-o-meter-pro</artifactId>
  <version>1.0</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <javafx.version>21.0.2</javafx.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>ReadOMeterApp</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

**3. Run**
```bash
mvn javafx:run
```

---

### Method 3 — Build a .exe Installer

> Requires: JDK 17+, JavaFX SDK, and WiX Toolset (Windows only)

**Step 1 — Compile and package a JAR**
```bash
javac --module-path "C:\javafx-sdk-21\lib" ^
      --add-modules javafx.controls ^
      -d out src\ReadOMeterApp.java

copy src\style.css out\

jar --create --file dist\ReadOMeterPro.jar ^
    --main-class ReadOMeterApp ^
    -C out .
```

**Step 2 — Run jpackage**
```bash
jpackage ^
  --input dist ^
  --dest installer ^
  --name "Read-O-Meter Pro" ^
  --main-jar ReadOMeterPro.jar ^
  --main-class ReadOMeterApp ^
  --type exe ^
  --module-path "C:\javafx-sdk-21\lib" ^
  --add-modules javafx.controls ^
  --win-shortcut ^
  --win-menu ^
  --app-version 1.0 ^
  --description "Real-time writing analytics desktop app"
```

**Step 3 — Run the installer**

Find `installer\Read-O-Meter Pro-1.0.exe` and double-click it.
The app installs like any normal Windows application — no Java required on the target machine.

---

## 📖 How to Use

1. **Type** directly into the large text area — stats update automatically after a short pause.
2. **Drag and drop** any `.txt` file onto the text area to load it instantly.
3. Read your stats in the right sidebar:
   - **Reading Time** — estimated at 260 words per minute.
   - **Word Count** — total tokenised words.
   - **Complexity** — Easy / Medium / Hard (colour-coded green / yellow / red).
   - **Tired Words** — lists filler words with frequency counts e.g. `very(3), just(1)`.
4. The **status bar** at the bottom confirms the last action (file loaded, analysis complete, errors).

---

## 🔬 Logic & Algorithms

### Reading Time
```
totalSeconds = ceil(wordCount / 260 * 60)
Display as "X min Y sec"
```

### Complexity Level
Based on the **average character length** of all words:

| Average Word Length | Level |
|---|---|
| ≤ 5.0 characters | 🟢 Easy |
| ≤ 7.0 characters | 🟡 Medium |
| > 7.0 characters | 🔴 Hard |

### Tired Word Detection
The following words are flagged as overused:
`very`, `just`, `really`, `quite`, `basically`, `literally`, `actually`, `stuff`, `things`, `nice`

Each is counted using a stream frequency map and displayed as `word(count)`.

### Tokenisation
Words are split on whitespace and stripped of non-alphabetic characters using:
```java
text.split("\\s+")
    .map(w -> w.replaceAll("[^a-zA-Z']", "").toLowerCase())
```

---

## 🗺 Roadmap

- [ ] Export analysis report as `.pdf`
- [ ] Sentence count and average sentence length
- [ ] Readability score (Flesch-Kincaid)
- [ ] Highlight tired words directly in the text
- [ ] Support for `.docx` and `.pdf` file input
- [ ] Undo / Redo history
- [ ] Font size controls in the editor
- [ ] Light mode toggle

---

## 🤝 Contributing

Contributions are welcome! Here's how:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m "Add: your feature description"`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request

Please keep code well-commented (this is a learning project) and follow the existing MVC structure.

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

```
MIT License — you are free to use, copy, modify, and distribute
this software for any purpose with attribution.
```

---


> Built with ☕ Java and 🎨 JavaFX — a portfolio project demonstrating MVC architecture, async concurrency, and modern Java syntax.
