# PraiseView

**Modern JavaFX alternative to OpenLP for church projection.**

PraiseView is an open-source projection software built specifically for Catholic churches and worship services. It offers a clean, modern JavaFX interface with powerful support for songs, prayers, announcements, service planning, and multi-monitor projection.

## ✨ Features

### Core Projection
- **Multi-monitor support** — Automatic detection of secondary monitor with full-screen projection.
- **Live preview** in the main operator window (aspect ratio matched to projection).
- **Blackout / Clear** screen controls with smart theme restoration.
- **Navigation** — Previous / Next item and verse controls with keyboard shortcuts.

### Content Management
- **Songs** — Rich song model with verses (Chorus, Coda, etc.), editor, and library.
- **Prayers** — Dedicated prayer management and editor.
- **Text Slides** — Support for custom text content.
- **Announcements** — Support for announcements.
- **Service Planner** — Build and manage the full order of service with drag-to-reorder or Up/Down buttons.
- **Media Support**:
  - Images
  - Videos with media controls (play/pause/rewind/forward)
  - Audio playback
  - PPT presentations with smooth fade transitions and proper slide numbering

### Themes & Customization
- **Dark theme** by default.
- **Theme Editor** — Create and manage custom themes (background images/videos, fonts, colors, text/title colors, logos, title display toggle).
- **Smart Theme Management** — Theme state persists correctly even after blackout or screen clear.
- **Video Backgrounds** — Support for video files as theme backgrounds.

### Operator Controls
- **Service File Management**:
  - New Service, Save, Save As, Load Service
  - Smart Save (direct save if file already loaded)
  - Window title displays current service file name
- **Keyboard Shortcuts**:
  - `DELETE` — Delete selected service item
  - `Ctrl+Up/Down` — Reorder service items
  - `LEFT/RIGHT Arrow` — Navigate through verses/slides/pages
- **Easy Reordering** — Up/Down buttons in addition to drag-to-reorder

### Mobile Companion App
- **Phone Remote Controller** (WebSocket-based)
  - Real-time service list browsing
  - Current verse/item list display
  - Direct projection from phone
  - Live content selection with immediate projection
  - **Note:** Mobile app is companion-only; animations and transitions are desktop-only

### Technical
- **SQLite** database for all data persistence.
- **Import / Export** for songs, prayers, texts, and services (JSON format).
- **WebSocket Server** for phone remote control integration.
- **High-quality PPT rendering** with antialiasing and bicubic interpolation.
- **Text Pagination** with intelligent word-wrapping and font sizing.
- **Cross-platform** (Maven + JavaFX 23+).

## 📋 Current Status

**Completed Features:**
- ✅ Core projection engine with theme support
- ✅ Song/Prayer/Text management with editors
- ✅ Service planning with smart reordering
- ✅ Multi-monitor projection
- ✅ Live preview with aspect ratio matching
- ✅ Theme customization (images, videos, fonts, colors)
- ✅ Media playback (images, videos, audio)
- ✅ PPT support with smooth transitions
- ✅ Phone remote controller (WebSocket)
- ✅ Import/Export functionality
- ✅ Keyboard shortcuts and accessibility
- ✅ File management (Save/Save As with window title)

**Known Limitations:**
- PPT animations are rendered as static images (no live animation playback)
- Mobile app is companion-only (no full feature parity with desktop)

## 🛣️ Roadmap

**Near Future:**
- Improved media library browsing
- More import formats (e.g., ChordPro, OpenLP export)
- Advanced PPT handling with slide thumbnails

**Future Enhancements:**
- Animations & Transitions — Smooth animations and transitions on projection screen (desktop only)
- AI Helper — Intelligent auto-advance based on cues, timing, or voice recognition
- Better PowerPoint integration with live animation playback
- Cloud sync for songs/services
- More theme presets and export options

## Screenshots

*(Add screenshots/GIFs of the main window, projection screen, song editor, theme editor, and phone remote here)*

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
  - `model/` — Data models (Song, Verse, Prayer, Theme, MediaItem, PptItem, etc.)
  - `controller/` — UI controllers (MainController, ProjectionController, ThemeEditorController, etc.)
  - `service/` — Business logic (JsonService, PhoneRemoteServer, UpdateService, etc.)
  - `db/` — Database layer (DatabaseService)
  - `util/` — Utilities (TextPaginationUtil, AppLogger, PptRenderer, etc.)
- `src/main/resources/` — FXML views, CSS, images, and icons

## Recent Updates (Latest Session)

### Phone Remote & Broadcast
- Enhanced PhoneRemoteServer with ObjectMapper and multi-client support
- Created ServiceListDTO and VerseListDTO for WebSocket data transfer
- Implemented sendServiceListToPhone() and sendVerseListToPhone() broadcast methods
- Full WebSocket protocol for real-time phone control

### Theme & Projection Fixes
- Fixed theme loss after blackout using themeHiddenByBlackout flag
- Fixed preview not updating after theme restoration
- Added theme state tracking for both projection and preview

### Content Editing
- Enabled verse editing in SongEditorDialog with "Update Verse" button

### PPT Enhancements
- Added "Slide X of Y" format to PPT items
- Implemented smooth fade transitions (200ms out, 300ms in) for PPT slides
- Improved PPT rendering quality with antialiasing and bicubic interpolation
- Removed file paths from current items list display

### Service Management
- Added smart Save (direct save if file loaded, otherwise Save As dialog)
- Implemented window title tracking showing current service file name
- Added "Save As" menu option with keyboard access
- DELETE key support for service list deletion
- Up/Down arrow buttons and Ctrl+Up/Down keyboard shortcuts for reordering

### Preview & Display
- Fixed stage view aspect ratio to match projection (maintained 16:9)
- Fixed text overflow issues with proper padding and width constraints
- Implemented proportional padding for text containers

## Contributing

Contributions are welcome! Feel free to:
- Report bugs or suggest features
- Submit Pull Requests
- Help improve documentation and testing

## Releases

Latest: **1.2.0** — Check the [Releases page](https://github.com/jasonfernandes420/PraiseView/releases).

## License

MIT License.

---

**More features coming soon!** Feedback and suggestions are highly appreciated — just open an issue.