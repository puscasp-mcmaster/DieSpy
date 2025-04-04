# Authentication Manager 
This module handles user authentication logic for the DieSpy app. It supports account creation, email verification, login, and password recovery

## Features
- Authentication, and account management tools
- Secure login with hashed passwords
- Password changing support
- Permanent account deletion 

## Connected UI
| UI                          | Description                                |
|-----------------------------|--------------------------------------------|
| `AuthManager.kt`            | Core class handling authentication actions |
| `LoginFragment.kt`          | UI and logic for user login                |
| `CreateAccountFragment.kt`  | UI and logic for account registration      |
| `ChangePasswordFragment.kt` | UI and logic for changing password         |

## Logic / Managers
| File                       | Description                                                                |
|----------------------------|----------------------------------------------------------------------------|
| `AuthenticationManager.kt` | Hashes passwords, and handles all firestore connections for authentication |

## Layouts 
| Layout                         | Description                               |
|--------------------------------|-------------------------------------------|
| `fragment_create_account.xml`  | Account creation screen                   |
| `fragment_settings`            | User setting screen with account deletion |
| `fragment_login`               | Main user login screen                    |
| `fragment_change_password.xml` | Change password screen                    |
