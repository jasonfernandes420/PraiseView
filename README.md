# PraiseView

**Modern JavaFX alternative to OpenLP for church projection.**

PraiseView is an open-source projection software built specifically for churches and worship services. It offers a clean, modern JavaFX interface with powerful support for songs, prayers, announcements, service planning, and multi-monitor projection.

## ✨ Features

### Core Projection
- **Multi-monitor support** — Automatic detection of secondary monitor with full-screen projection.
- **Live preview** in the main operator window.
- **Blackout / Clear** screen controls.
- **Navigation** — Previous / Next item and verse controls.

### Content Management
- **Songs** — Rich song model with verses (Chorus, Coda, etc.), editor, and library.
- **Prayers** — Dedicated prayer management and editor.
- **Announcements** — Support for announcements.
- **Service Planner** — Build and manage the full order of service.
- **Media Support**:
  - Images
  - Videos
  - PPT presentations (basic support)

### Themes & Customization
- **Dark theme** by default.
- **Theme Editor** — Create and manage custom themes (backgrounds, fonts, colors, logos).

### Technical
- **SQLite** database for all data persistence.
- **Import / Export** for songs, prayers, and services.
- **Cross-platform** (Maven + JavaFX 21+).

## 📋 Current Status

The core projection engine, song/prayer management, theme system, and basic service planning are implemented and functional.

**Planned / In Progress:**
- Improved media library and PPT handling
- More import formats (e.g., ChordPro, OpenLP export)
- Enhanced UI/UX polish

## 🛣️ Roadmap

- **Animations & Transitions** — Smooth animations and transitions when changing slides/items on the projection screen.
- **Mobile App Companion** — A companion mobile app (Android/iOS) for remote control, lyrics display, and service viewing.
- **AI Helper** — Intelligent auto-advance feature that automatically changes slides based on cues, timing, or voice recognition.

Additional future ideas:
- Better PowerPoint integration (slide thumbnails + live control)
- Background video support
- More theme presets and export options
- Cloud sync for songs/services

## Screenshots

*(Add screenshots/GIFs of the main window, projection screen, song editor, and theme editor here)*

## 🚀 How to Run (Development)

1. Clone the repository:
   ```bash
   git clone https://github.com/jasonfernandes420/PraiseView.git
   cd PraiseView
   ```

2. Run the app:
   ```bash
   mvn clean javafx:run
   ```

## 📦 How to Build / Package

```bash
mvn clean package
```

For native installers, use `jpackage`:

```bash
jpackage --input target --name PraiseView --main-jar praiseview-*.jar \
  --main-class com.praiseview.Launcher --type msi
```

See `.github/workflows/` for CI build configuration.

## Project Structure

- `src/main/java/com/praiseview/`
  - `model/` — Data models (Song, Verse, Prayer, Theme, etc.)
  - `controller/` — UI controllers
  - `service/` — Business logic & persistence
  - `db/` — Database layer
- `src/main/resources/` — FXML views, CSS, images

## Contributing

Contributions are welcome! Feel free to:
- Report bugs or suggest features
- Submit Pull Requests
- Help improve documentation and testing

## Releases

Latest: **1.1.1** — Check the [Releases page](https://github.com/jasonfernandes420/PraiseView/releases).

## License

MIT License.

---

**More features coming soon!** Feedback and suggestions are highly appreciated — just open an issue.