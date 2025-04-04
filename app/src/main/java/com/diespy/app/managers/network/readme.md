# Network Manager / Public Network Manager
Handles establishing local device connection with Bluetooth Low Energy (BLE). Used to join nearby parties without a passcode.
NetworkManager contains all the functionality, PublicNetworkManager is just the public interface used to be safe across different threads and screens.

## Features
- Broadcasts party name with BLE in order to minimize power and network usage.
- Broadcasts with a 16-bit UUID in order to minimize packet size.
- Listens for available parties being broadcasted, filters duplicate parties from different sources
- Provides consistent broadcasting across different screens in a party
- Implements locking mechanism to not broadcast twice to minimize overhead

## Connected UI
| UI                     | Description                                                            |
|------------------------|------------------------------------------------------------------------|
| `JoinPartyFragment.kt` | Screen displaying nearby parties and interfact to detect and join them |
| `HomeFragment.kt`      | Main party screen, used to broadcast party status to anyone nearby     |
| `PartyFragment.kt`     | Home Screen, closes all broadcasts after leaving a party               |

## Logic / Managers
| File                      | Description                                                                                                |
|---------------------------|------------------------------------------------------------------------------------------------------------|
| `NetworkManager.kt`       | Provides all BLE network functions, allowing users to broadcast or detect other parties.                   |
| `PublicNetworkManager.kt` | Provides a thread-safe singleton instance of a NetworkManager to be consistent across different fragments. |

## Layouts (Usage Example)
| Layout                    | Description                                       |
|---------------------------|---------------------------------------------------|
| `fragment_join_party.xml` | Screen that displays the list of joinable parties |
