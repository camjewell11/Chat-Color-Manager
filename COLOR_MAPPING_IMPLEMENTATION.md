# Chat Color Manager - Color Mapping Implementation

## Overview

The Chat Color Manager plugin has been completely refactored to support **colorblind-friendly color remapping**. Instead of applying fixed colors to chat types, users can now create custom mappings to remap any color that appears in chat messages to any other color of their choice.

## New Architecture

### Files Added/Modified

#### 1. **ColorMapping.java** (NEW)

- Represents a single color remapping
- Stores: original color (hex), new color (hex), and label
- Methods to convert between hex and Color objects
- Used by the UI and config system

#### 2. **ColorMappingPanel.java** (NEW)

- Custom Swing JPanel for the UI
- Features:
  - **Add Mapping Button** - Opens dialog with color pickers
  - **Edit Selected Button** - Modify existing mappings
  - **Remove Selected Button** - Delete mappings
  - **Color Pickers** - Uses Java's JColorChooser for selecting colors
  - **Visual Swatches** - Shows color mappings with visual indicators
  - **List Display** - Shows all active mappings with original → new color visualization

#### 3. **ChatColorManagerConfig.java** (UPDATED)

- Added `useColorMappings()` - Toggle to enable/disable color mapping mode
- Added `colorMappingsJson()` - Stores all color mappings as JSON array
- Kept legacy color config methods for backward compatibility

#### 4. **ChatColorManagerPlugin.java** (UPDATED)

- **New event handling:**
  - Loads color mappings from config on startup
  - Subscribes to config changes to reload mappings
  - Intercepts all chat messages and applies color remappings
- **Core functionality:**
  - `loadColorMappings()` - Parses JSON from config into ColorMapping objects
  - `saveColorMappings()` - Serializes mappings back to JSON config
  - `rebuildRemapCache()` - Creates fast lookup map (original hex → new hex)
  - `applyColorRemappings()` - Uses regex to find and replace `<col=RRGGBB>` tags
- **UI Integration:**
  - Creates a toolbar button that opens the ColorMappingPanel
  - Panel allows dynamic add/remove/edit of color mappings
  - Changes immediately reflected in chat color rendering

## How It Works

### Color Detection & Remapping

1. All chat messages in RuneLite contain color codes in the format: `<col=RRGGBB>text</col>`
2. When a chat message arrives, the plugin:
   - Extracts all color tags using regex: `<col=([0-9A-Fa-f]{6})>`
   - Checks if each color has a mapping in the user's custom list
   - Replaces the original color code with the mapped color
   - Message displays with new colors in-game

### User Workflow

1. Click the toolbar button labeled "Color Mappings"
2. The ColorMappingPanel opens with existing mappings (if any)
3. Click **"+ Add Mapping"** to create a new remapping
4. A dialog appears with two color pickers:
   - **Original Color**: The color in chat you want to change (with visual preview)
   - **New Color**: What you want that color to become (with visual preview)
5. Optionally name the mapping (e.g., "Red to Green" for protanopia assistance)
6. Click **Create** - immediately starts remapping in chat
7. Can edit or remove mappings at any time

## Colorblind Assistance Features

The plugin supports typical colorblind variations:

### Protanopia (Red-Blind)

- Remap red colors → green, blue, or yellow
- Example: `#FF0000` → `#00FF00`

### Deuteranopia (Green-Blind)

- Remap green colors → red, blue, or gray
- Example: `#00FF00` → `#FF0000`

### Tritanopia (Blue-Yellow Blind)

- Remap yellow/blue → red, green, or gray
- Example: `#FFFF00` → `#FF00FF`

### Achromatomaly (Full Colorblindness)

- All colors → grayscale variants
- Example: `#FF0000` → `#808080`

## Technical Details

### Color Format

- RuneLite uses hex format: `<col=RRGGBB>` (no # prefix)
- Plugin stores internally as uppercase: `FF0000`
- JColorChooser returns java.awt.Color objects, converted to hex for storage

### Performance

- Color mappings cached in memory as a HashMap for O(1) lookup
- Regex pattern compiled once as static field
- Config changes trigger minimal overhead (just rebuild cache)

### Data Persistence

- Mappings stored as JSON array in RuneLite config
- Format: `[{"originalColor":"RRGGBB","newColor":"RRGGBB","label":"Name"}]`
- Survives plugin reload and game restarts
- Easy to export/share configurations

### Debug Mode

- Set `debugMode: true` in config
- Logs all intercepted messages and color remappings to console
- Useful for troubleshooting color codes

## Usage Tips

1. **Finding Colors to Remap**
   - Check chat messages for colors that are hard to see
   - Use debug mode to see the exact color codes in chat
   - Create a mapping for that exact color

2. **Testing Mappings**
   - Create a test mapping to ensure it works
   - Send test messages in game to verify new color
   - Adjust if needed and edit the mapping

3. **Sharing Configurations**
   - Export the JSON from `colorMappingsJson` config value
   - Share with other colorblind players
   - They can paste it into their config directly

## Future Enhancements (Potential)

- Color picker eyedropper tool to click colors directly in chat window
- Preset colorblind mode templates (auto-select common remappings)
- Color similarity detection (suggest remappings for similar colors)
- Undo/redo support for mappings
- Export/import configuration files
- Color mapping profiles (save multiple sets for different purposes)

## Troubleshooting

**Colors not remapping?**

- Check `useColorMappings` is enabled
- Verify mapping colors are in correct hex format (RRGGBB, not #RRGGBB)
- Enable debug mode to see actual color codes in messages

**Plugin won't compile?**

- Ensure Gson is available (included with RuneLite client)
- Check Java 11+ is installed
- Run `./gradlew clean build` to rebuild

**Toolbar button missing?**

- Check that ClientToolbar is properly injected
- Verify plugin is enabled in RuneLite settings
- Monitor console for initialization errors
