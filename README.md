# Band Stats — native Android

Same dashboard as the Band Stats web app, but it talks to your **Mi Band / Amazfit** over **native Android Bluetooth**. There is **no file upload**, **no Gadgetbridge zip**, **no account**, and **no cloud**.

## What it does

1. You paste the band **auth key** once (32 hex characters from Zepp / Gadgetbridge / huami-token).
2. Tap **Connect**. The phone scans, pairs over BLE, authenticates, then reads:
   - live steps, distance, calories
   - battery
   - streaming heart rate
3. Values are stored on the phone in a local Room database and shown on the dashboard (today card, week chart, vitals, body log, journal).

Sleep stages and full workout history are **not** available from live BLE the way they are from a Gadgetbridge export. This app only keeps what the band actually sends over Bluetooth, plus anything you log by hand.

## Open in Android Studio

1. Open Android Studio → **Open** → this folder (`BandStatsAndroid`).
2. Let Gradle sync.
3. Run on a real phone (BLE does not work on the emulator).
4. Grant Bluetooth (and notifications) when asked.

Min SDK 26, target SDK 35, Kotlin + Jetpack Compose.

## Auth key

Must be exactly 16 bytes written as 32 hex characters, for example:

`a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4`

Optional `0x` prefix is accepted. Do not unpair the band from Zepp/Gadgetbridge after you copy the key.

## Project layout

```
app/src/main/java/com/example/miband5/
  ble/          native BLE scan, GATT, Mi Band auth, parsers
  data/         Room database (days, HR samples, manual workouts)
  service/      foreground BLE sync service
  sync/         live poll + HR stream → Room
  ui/           Band Stats dashboard (Compose)
```
