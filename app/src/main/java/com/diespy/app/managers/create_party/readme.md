# Create Party Manager
Handles party creation logic including join password generation and Firestore setup.

## Features
- Creates new party documents in Firestore
- Automatically generates secure join passwords
- Associates party with initial user
- Returns party ID for further use

## Logic / Managers
| File                    | Description                                              |
|-------------------------|----------------------------------------------------------|
| `CreatePartyManager.kt` | Core class for handling creation of parties in Firestore |

## Connected UI
| UI                          | Description                                  |
|-----------------------------|----------------------------------------------|
| `CreatePartyFragment.kt`    | UI screen for user to create a new party     |

## Layouts
| Layout                         | Description                               |
|--------------------------------|-------------------------------------------|
| `fragment_create_party.xml`    | Layout for creating a new party           |
