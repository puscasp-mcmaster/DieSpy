# Shared Preferences Manager
Manages local data storage for user and party session info using Android's `SharedPreferences`.

## Features
- Save and retrieve current user ID and username
- Save and retrieve current party ID, name, and member count
- Clear data for current user, current party, or all stored preferences 
- Used during login, account creation, and logout flows
- Supports persistent session management and navigation
- Helps restore state when app is resumed or relaunched

## Persistently Stored Values
| Key                          | Description                          |
|------------------------------|--------------------------------------|
| `currentUserId`              | Unique identifier for the user       |
| `currentUsername`            | Display name of the user             |
| `currentPartyId`             | Party ID user is currently in        |
| `currentPartyName`           | Party name user is currently in      |
| `currentPartyUserCount`      | Number of users in the current party |

