# PraiseView Phone Remote Control Protocol

## Overview
The Phone Remote Controller uses WebSocket connections to allow mobile devices to remotely control projections in PraiseView.

**WebSocket Connection:** `ws://{ip}:{port}/praiseview`

---

## Command Types

### 1. Text Commands (Simple)
Simple text commands for basic controls.

#### Available Commands:
```
- "next" → Next verse/slide
- "previous" or "prev" → Previous verse/slide
- "start" or "project" → Start projection
- "blackout" → Blackout screen
- "clear" → Clear screen
- "playpause" or "play-pause" → Play/pause media
- "rewind" → Rewind media
- "forward" → Forward media
- "get-services" → Request service list
```

#### Example:
```json
"next"
```

---

### 2. JSON Commands (Advanced)
JSON commands with parameters for service and verse selection.

#### Get Service List
Request all items in the service queue.

```json
{
  "command": "get-services"
}
```

**Response:**
```json
{
  "type": "service_list",
  "data": {
    "services": [
      {
        "id": "uuid-1",
        "index": 0,
        "title": "Amazing Grace",
        "type": "SONG"
      },
      {
        "id": "uuid-2",
        "index": 1,
        "title": "Our Father",
        "type": "PRAYER"
      }
    ],
    "current_index": 0
  }
}
```

---

#### Select Service (Song/Prayer/Slide)
Select a service item and receive its verses/pages.

```json
{
  "command": "select-service",
  "index": 0
}
```

**Response:**
```json
{
  "type": "verse_list",
  "data": {
    "service_title": "Amazing Grace",
    "content_id": "uuid-1",
    "verses": [
      {
        "index": 0,
        "label": "Verse 1",
        "preview": "Amazing grace, how sweet the sound...",
        "content": "Amazing grace, how sweet the sound..."
      },
      {
        "index": 1,
        "label": "Verse 2",
        "preview": "Twas grace that taught my heart to fear...",
        "content": "Twas grace that taught my heart to fear..."
      },
      {
        "index": 2,
        "label": "Chorus",
        "preview": "Through many dangers, toils and snares...",
        "content": "Through many dangers, toils and snares..."
      }
    ],
    "current_verse_index": 0
  }
}
```

---

#### Select Verse (Project Directly)
Select and project a specific verse/page from a content item (Song, Prayer, etc.).

⚠️ **Important:** Must include both `content_id` (Song ID, Prayer ID, etc.) and `verse_index` to identify which verse to project.

```json
{
  "command": "select-verse",
  "content_id": "song-uuid-or-prayer-uuid-or-media-id",
  "verse_index": 2
}
```

**Response:**
```json
{
  "status": "ok"
}
```

---

## Data Models

### ServiceListDTO
Sent when service list is requested.

```java
{
  "services": [
    {
      "id": "string - UUID of service",
      "index": "int - position in queue",
      "title": "string - display name",
      "type": "string - SONG, PRAYER, TEXT, IMAGE, VIDEO, PPT"
    }
  ],
  "current_index": "int - currently selected service index"
}
```

---

### VerseListDTO
Sent when a service is selected.

```java
{
  "service_title": "string - name of selected service",
  "content_id": "string - UUID of the actual content (Song ID, Prayer ID, etc.)",
  "verses": [
    {
      "index": "int - position in verses",
      "label": "string - Verse 1, Chorus, Page 2, etc.",
        "preview": "string - first 100 chars of content",
        "content": "string - full content of the projected verse/page"
    }
  ],
  "current_verse_index": "int - currently displayed verse"
}
```

---

## Complete Workflow Example

### Step 1: Connect
```
WebSocket Connect: ws://192.168.1.100:8080/praiseview
```

### Step 2: Get Service List
```json
← Send:
{
  "command": "get-services"
}

→ Receive:
{
  "type": "service_list",
  "data": {
    "services": [
      {"id": "a1b2c3", "index": 0, "title": "Amazing Grace", "type": "SONG"},
      {"id": "d4e5f6", "index": 1, "title": "Our Father", "type": "PRAYER"}
    ],
    "current_index": -1
  }
}
```

### Step 3: Select Service
```json
← Send:
{
  "command": "select-service",
  "index": 0
}

→ Receive:
{
  "type": "verse_list",
  "data": {
    "service_title": "Amazing Grace",
    "content_id": "a1b2c3",
    "verses": [
      {"index": 0, "label": "Verse 1", "preview": "Amazing grace, how sweet..."},
      {"index": 1, "label": "Verse 2", "preview": "Twas grace that taught..."},
      {"index": 2, "label": "Chorus", "preview": "Through many dangers..."}
    ],
    "current_verse_index": 0
  }
}
```

### Step 4: Select Verse to Project
```json
← Send:
{
  "command": "select-verse",
  "content_id": "a1b2c3",
  "verse_index": 1
}

→ Receive:
{
  "status": "ok"
}
```

---

## Error Handling

### Invalid Command
```json
{
  "status": "error",
  "message": "Unknown command"
}
```

### Invalid Service Index
```
AppLogger: "Invalid service index: 5"
```

### Invalid Verse Index
```
AppLogger: "Invalid verse index: 10 (max: 3)"
```

### Content Not Found
```
AppLogger: "Content not found with ID: invalid-uuid"
```

---

## Implementation Notes

### Key Features:
- ✅ Content ID is required for verse selection (identifies Song/Prayer/Media)
- ✅ Works with any Projectable type (Song, Prayer, Text, Image, Video, PPT)
- ✅ Verse index is validated against the content's maximum verses
- ✅ All commands are case-insensitive
- ✅ JSON and text commands are both supported
- ✅ Supports multiple concurrent phone connections
- ✅ All errors are logged for debugging

### Why Content ID?
Including the `content_id` in the `select-verse` command ensures:
1. **Direct Content Identification**: Identifies the exact Song, Prayer, TextSlide, MediaItem, or PptItem
2. **Robustness**: Works even if service queue order changes
3. **Validation**: Server can verify the verse exists in the specified content
4. **Multi-Client**: Multiple clients can work with different content items
5. **Type-Safe**: Works with any Projectable type

---

## Architecture

### Component Flow:
```
Phone Client
    ↓ (WebSocket JSON)
PhoneRemoteServer.handleMessage()
    ↓ (Parse JSON)
MainController.handleRemoteCommand()
    ↓ (Route to handler)
- handleJsonRemoteCommand()
- handleSimpleRemoteCommand()
    ↓ (Execute action)
- sendServiceListToPhone()
- selectServiceAndSendVerses()
- selectVerseAndProject()
    ↓
PhoneRemoteServer.sendServiceListToClients()
PhoneRemoteServer.sendVerseListToClients()
    ↓ (WebSocket JSON)
Phone Client (Display updated data)
```

---

## Files Modified

1. **PhoneRemoteServer.java**
   - Added JSON serialization with ObjectMapper
   - Added `sendServiceListToClients()`
   - Added `sendVerseListToClients()`
   - Updated message handling to pass full JSON

2. **MainController.java**
   - Added `handleJsonRemoteCommand()`
   - Added `handleSimpleRemoteCommand()`
   - Added `sendServiceListToPhone()`
   - Added `selectServiceAndSendVerses()`
   - Added `selectVerseAndProject(String serviceId, int verseIndex)`
   - Added `sendVerseListToPhone()`

3. **New Files:**
   - `ServiceListDTO.java` - Service list data transfer object
   - `VerseListDTO.java` - Verse list data transfer object
