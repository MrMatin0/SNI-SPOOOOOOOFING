# SNI-Spoofing for Android

Deep packet inspection (DPI) bypass and TLS ClientHello decoy injection application built with Kotlin and Jetpack Compose for Android.

```
███████╗███╗   ██╗██╗   S P O O F I N G   v2.0 (Android)
██╔════╝████╗  ██║██║   DPI Bypass · TLS Decoy Injection
███████╗██╔██╗ ██║██║
```

## Features

- **Local TCP & SNI Proxy Engine**: Operates a local TCP proxy using Kotlin Coroutines that injects a **fake TLS ClientHello** carrying a decoy SNI.
- **Decoy TLS 1.3 ClientHello Generator**: Custom binary payload builder creating standard 517-byte ClientHello records with customizable SNI, 32-byte cryptographic entropy, session IDs, and padding.
- **Material 3 Control Center**:
  - **Dashboard**: Live proxy toggle, bypass verdict status ("Bypass Confirmed", "Degraded", "Blocked"), real-time upload/download speed gauge, and activity feed.
  - **Config Panel**: Configurable local listener host/port, target destination IP/port, fake SNI domain input with popular decoy presets (Vercel, Cloudflare, Wikipedia, MCI, etc.), and bypass strategy options.
  - **Active Tunnels Monitor**: Real-time list of active TCP streams, endpoints, transfer statistics, and setup latency (ms).
  - **Decoy Packet Inspector**: Visual breakdown of generated binary TLS 1.3 ClientHello records with copyable raw hex payloads.
  - **DPI Diagnostics & Tester**: Active connection handshake tester that tests whether middleboxes drop or accept decoy packets.
- **Local Database Persistence**: Powered by Room for saving proxy configurations and event logs.

## Architecture

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose + Material 3
- **Data Layer**: Room Database (`AppDatabase`, `ConfigDao`, `LogDao`)
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Navigation Compose with `@Serializable` type-safe routes

## Support

If this tool helps you reach the free internet, consider supporting future development:

- **USDT (BEP20)**: `0x76a768B53Ca77B43086946315f0BDF21156bF424`
- **USDT (TRC20)**: `TU5gKvKqcXPn8itp1DouBCwcqGHMemBm8o`
- **Telegram**: [@patterniha](https://t.me/patterniha) · [@projectXhttp](https://t.me/projectXhttp)
