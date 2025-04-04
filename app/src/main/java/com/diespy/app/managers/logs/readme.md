# Log Manager
Handles saving, retrieving, updating, and deleting player roll logs in Firestore. Useful for audit trails and roll history.

## Features
- Save roll logs to Firestore
- Fetch all logs for a given party
- Update specific logs
- Delete specific logs
- Realtime Firestore subscriptions for auto-updating logs

## Logic / Managers
| File            | Description                                                         |
|-----------------|---------------------------------------------------------------------|
| `LogManager.kt` | Manages all operations related to logging player rolls              |
| `LogMessage`    | Data class representing each log entry with ID, user, and timestamp |

## Layouts (Usage Example)
| Layout                 | Description                                      |
|------------------------|--------------------------------------------------|
| `fragment_logs.xml`    | Screen that displays the list of party roll logs |
