# Camera Manager 
Initializes Camera sends frames to be processed. CameraX is the main package that is utilized for image capture. Sends frames to be processed and calculates required sums and counts for user display.  

## Features
- Camera setup 
- Frame capture 
- Image processing
- Bounding boxes for image processing 
- Image processing 
- Camera permissions 
- Calculations for dice

## Connected UI
| UI                          | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| `BoundingBoxOverlay.kt`     | Draws bounding boxes for dice that it utilized later for detection          |
| `DiceDetectionFragment.kt`  | Handles calculations and display for dice. Connection between camera and ML |

## Logic / Managers
| File               | Description                                                |
|--------------------|------------------------------------------------------------|
| `CameraManager.kt` | Preprocesses frames, initializes camera for dice detection |

## Layouts 
| Layout                         | Description                               |
|--------------------------------|-------------------------------------------|
| `fragment_dice_detection.xml`  | Main game screen, captures and shows roll |
