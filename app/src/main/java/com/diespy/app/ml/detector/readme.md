# Dice Detector
Handles real-time inference for dice detection using a TensorFlow Lite model. Captures and preprocesses frames, runs inference, and returns bounding boxes with class and confidence.

## Features
- Loads TensorFlow Lite model and metadata
- Performs image preprocessing and normalization
- Detects dice faces with bounding box and class
- Supports GPU or CPU inference
- Thread-safe detection with locking
- Configurable confidence threshold

## Connected UI / Usage
- Invoked from `DiceDetectionFragment.kt` to perform inference when frames are captured
