# Model Files

Handles live dice detection using CameraX, TensorFlow Lite, and a SORT-based tracker to ensure accurate and stable results across frames.

## Features
- Camera frame capture using CameraX
- TensorFlow Lite dice detection model
- Euclidean distance matching across frames
- SORT-style tracking of dice
- Bounding box overlays
- Frame smoothing for consistent predictions

## Connected UI
| UI                          | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| `DiceDetectionFragment.kt`  | Main logic for detection, tracking, and final roll processing               |
| `BoundingBoxOverlay.kt`     | Draws detection boxes over live camera feed                                 |

## Supporting Classes
| File                 | Description                                                      |
|----------------------|------------------------------------------------------------------|
| `DiceDetector.kt`    | Handles inference using TFLite model and bounding box extraction |
| `TrackedDice.kt`     | Keeps track of each die's position and history across frames     |
| `MatchCandidate.kt`  | Utility for comparing tracked dice to current detections         |
| `DiceBoundingBox.kt` | Represents a single detection result from the model              |

## Layouts
| Layout                        | Description                                           |
|-------------------------------|-------------------------------------------------------|
| `fragment_dice_detection.xml` | Live detection screen with camera preview and buttons |
