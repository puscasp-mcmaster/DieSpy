# Join Party Manager
Handles party joining logic by party code

## Features
- Adds user to existing party documents in Firestore
- Verifies party document exists in Firebase

## Logic / Managers
| File                  | Description                                             |
|-----------------------|---------------------------------------------------------|
| `JoinPartyManager.kt` | Core class for handling joining of parties in Firestore |

## Connected UI
| UI                    | Description                            |
|-----------------------|----------------------------------------|
| `JoinPartyFragment.kt` | UI screen for user to join a new party |

## Layouts
| Layout           | Description                    |
|------------------|--------------------------------|
| `join_party.xml` | Layout for joining a new party |
