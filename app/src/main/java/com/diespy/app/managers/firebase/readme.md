# Firestore Manager

Handles all Firestore related operations such as creating, updating, querying, and deleting documents. Also includes logic to manage party memberships and preload data into cache for quicker access.

## Features
- Create, update, and delete Firestore documents
- Query documents by field or ID
- Check for document existence
- Retrieve all parties for a user
- Retrieve usernames for a party
- Manage user membership in parties
- Preload party data into local cache

## Connected UI
| UI                    | Description                                    |
|-----------------------|------------------------------------------------|
| `PartyFragment.kt`    | UI that displays and interacts with parties    |
| `HomeFragment.kt`     | UI that loads user's party data on startup     |


## Firestore Collections Used
- `Users`
- `Parties`
    - Subcollections:
        - `logs`
        - `chat`