# 📂 File Organizer Pro

![Build Status](https://img.shields.io/github/actions/workflow/status/1Batrex1/file-organizer/build.yml?branch=main)
![Java Version](https://img.shields.io/badge/Java-25%2B-ED8B00?logo=openjdk&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

> **Stop organizing files manually.** File Organizer watches your folders and automatically sorts files into subdirectories based on your rules. Runs quietly in the background.

---

## ✨ Key Features

* **🚀 Real-time Monitoring:** Uses Java `WatchService` to detect new files instantly.
* **🧹 Manual Scan:** One-click cleanup for existing clutter in your folders.
* **🎨 Modern UI:** Clean interface built with **JavaFX** and **BootstrapFX**.
* **👻 Background Mode:** Minimizes to System Tray (doesn't clutter your taskbar).
* **⚙️ Hot-Swappable Config:** Add or remove rules without restarting the app.
* **📦 Cross-Platform:** Native installers for Windows (`.exe`), macOS (`.dmg`), and Linux (`.deb`).

---


## 📥 Installation

Go to the [**Releases**](https://github.com/1Batrex1/file-organizer/releases) page and download the installer for your system:

* **Windows:** Download `.exe`
* **macOS:** Download `.dmg`
* **Linux:** Download `.deb`

*No Java installation required! The app comes with a bundled lightweight Java runtime.*

---

## 🛠️ How it Works

1.  **Select a folder** to watch (e.g., `Downloads`).
2.  **Define rules**:
    * Extension: `pdf` ➔ Destination: `C:\MyFiles\Documents`
    * Extension: `jpg` ➔ Destination: `C:\MyFiles\Images`
3.  Click **"Save & Apply"**.
4.  Minimize the app. It will sit in your System Tray and organize any new file that lands in the folder.

---

## 🏗️ Tech Stack

* **Language:** Java 25
* **GUI Framework:** JavaFX 21
* **Styling:** BootstrapFX (Twitter Bootstrap for JavaFX)
* **JSON Processing:** Jackson
* **Native Integration:** JNA (Java Native Access)
* **Build Tool:** Maven
* **Installer Generator:** `jpackage` (via GitHub Actions)

---