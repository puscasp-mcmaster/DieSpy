# Dice Stats Manager
A utility class for tracking dice roll statistics across frames or sessions. Useful for aggregating dice counts and tracking totals.

## Features
- Tracks total roll sum
- Tracks number of dice detected
- Maintains count of each dice face (1–12)
- 1-6 are dotted die 
- 7-12 are numbered die (still numbers 1-6)

## Logic / Managers
| File                  | Description                                             |
|-----------------------|---------------------------------------------------------|
| `DiceStatsManager.kt` | Manages roll statistics for dice detection and analysis |
