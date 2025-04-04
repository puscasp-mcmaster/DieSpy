# Party Manager
Handles logic for managing party state including turn order, party members, and latest logs.

## Features
- Subscribe to latest roll logs
- Manage turn order with real-time updates
- Track and update party members
- Transactional turn rotation
- Integration with Firestore and caching system

## Logic / Managers
| File                   | Description                                                   |
|------------------------|---------------------------------------------------------------|
| `PartyManager.kt`      | Main class managing party logic like members and turn order   |
| `LogMessage.kt`        | Data class for individual roll logs shown in the party screen |
| `PartyCacheManager.kt` | Caches party state (user IDs, usernames, current turn)        |

## Connected UI
| UI                    | Description                                                            |
|-----------------------|------------------------------------------------------------------------|
| `PartyFragment.kt`    | Displays party name, turn order, current roll, and handles drag & drop |
| `TurnOrderAdapter.kt` | Adapter for displaying and updating turn order in real time            |

