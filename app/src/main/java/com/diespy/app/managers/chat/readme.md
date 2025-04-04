# Chat Manager
Handles real-time chat functionality between players using Firebase Firestore.

## Features
- Send and receive messages in real-time
- Messages ordered by timestamp
- Firestore snapshot listener for updates
- Timestamp formatting
- Auto-scroll to latest message
- Per-party chat handling

## Connected UI
| UI                  | Description                                              |
|---------------------|----------------------------------------------------------|
| `ChatFragment.kt`   | Chat screen displaying messages and handling user input  |
| `ChatAdapter.kt`    | Binds chat messages to the RecyclerView                  |

## Logic / Managers
| File             | Description                                   |
|------------------|-----------------------------------------------|
| `ChatManager.kt` | Handles Firestore message loading & listeners |

## Layouts
| Layout                  | Description                      |
|-------------------------|----------------------------------|
| `fragment_chat.xml`     | Layout for chat screen UI        |
