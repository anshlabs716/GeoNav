# Development

This page describes how to set up a development environment and work on
GeoNav.

## Requirements

- Android Studio (latest stable)
- JDK 17
- Android SDK with the target API

## Getting the code

```bash
git clone https://github.com/anshlabs716/GeoNav.git
cd GeoNav
```

## Build

```bash
./gradlew assembleDebug
```

## Run

```bash
./gradlew installDebug
```

## Test

```bash
./gradlew test
```

## Workflow

- Branch from `main` (see [CONTRIBUTING.md](../CONTRIBUTING.md)).
- Keep changes focused and documented.
- Update `CHANGELOG.md` for user-facing changes.
