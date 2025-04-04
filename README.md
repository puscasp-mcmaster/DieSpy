# DieSpy

**DieSpy** is a powerful Android app designed to detect and count dice rolls using machine learning — all in real time through a mobile camera. The app also features collaborative party functionality, chat support, and persistent roll logs, making it perfect for tabletop gaming sessions with friends, both in person and across the world.

---

## Features

- **Dice Detection**  
  Real time detection using your phone camera powered by TensorFlow Lite and a custom YOLOv10 model.

- **Accurate Roll Counting**  
  Post processing ensures high confidence roll detection (95%+ accuracy using cross referencing over multiple frames).

- **Account Creation**
  Create and login to your own account with persistent logins.  

- **Party System**  
  Join or host parties with friends. Every party state synced to all party members in real time.

- **Chat & Logs**  
  Communicate in app and access full roll history for respective parties at any time.

- **Dice Simulator**  
  Roll virtual dice when no physical dice are available.

---

## Tech Stack

| Layer | Technology |
|------|-------------|
| Language | Kotlin + XML |
| ML Model | YOLOv10 (via Ultralytics, TensorFlow Lite) |
| Backend | Firebase Firestore |
| IDE | Android Studio |

---

## Testing

All testing results can be found in the Verification and Validation document

---

## Main Project Structure

Highlighted below is the main coding structure for the project.
The standard flow for each component includes a xml file for the layout/design of the screen, a ui fragment, and its respective backend managers
For example, our Create Party Screen, has a fragment_create_party.xml (in res/layout), a CreatePartyFragment.kt (in java/ui/create_party), and a CreatePartyManager.kt (in managers/create_party)

```
DieSpy/
├── app/
│   ├── src/
│       ├── java/
|           ├── managers/      # backend managers for all components
|           ├── ml/            # machine learning backend managers for the detection model integration
|           ├── ui/            # all ui fragments (frontend code for each screeen)
|           ├── MainActivity   # app start up file
│       ├── res/
|           ├── drawable/      # xml drawbales as ui design helpers
|           ├── layout/        # xml layout files for ui design
|           ├── navigation/    # graph for in app page navigation
|
├── Documents/                 # all project realted documents
|
├── ml/                        # all code related to machine learning training
|


```

---

##  Getting Started

## Use APK:

TODO

Or if youd prefer to run as a developer,

## Build & Run:
1. Open in Android Studio
2. Sync Gradle
3. Connect a device or emulator
4. Run `app`


## Test Credentials
1. Login using Username: CapstoneTest, Password: 4ZP6AB
2. A mock party will be setup for you to join on the home page
3. You can also join a different mock party with join code: J1Qubb through the join party screen

All other features are free for exploratory testing

Once ready to start detecting die, you can use any real die, or use our test image set here: [drive link](https://drive.google.com/drive/folders/1QGVqBwHcTLTuscgx2YIgEt5LUxCrjM93?usp=drive_link)

---

## Contributors

- **Christian Majid**
- **Wyatt Habinski**
- **Jackson Cassidy**
- **Paul Puscas**

---

## License

This project is for academic purposes and is not yet publicly licensed.

---
