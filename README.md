# ZeroLauncher Bootstrapper

Auto-update bootstrapper for ZeroLauncher. Checks GitHub for the latest version on startup, prompts the user to update if available, and launches the correct JAR.

## Project Structure

```
ZeroLauncherBootstrapper/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── zerolauncher/
                    └── LauncherBootstrapper.java
```

## Setup

1. Replace `VERSION_URL` in `LauncherBootstrapper.java` with your GitHub Gist raw URL pointing to a plain `version.txt` (e.g. `1.2.3`).
2. Replace `DOWNLOAD_BASE_URL` with your GitHub Releases download base URL.
3. Place `Updater.exe` alongside the bootstrapper JAR when distributing.

## Build

```bash
mvn package
```

The output JAR will be at `target/ZeroLauncherBootstrapper.jar`.

## Run

```bash
java -jar target/ZeroLauncherBootstrapper.jar
```

## How It Works

1. Looks in `%APPDATA%\.zerolauncher\` for an existing `ZeroLauncher_X.Y.Z.jar`.
2. Fetches the latest version string from your GitHub Gist.
3. If no local JAR → downloads and runs it fresh.
4. If a local JAR exists and a newer version is available → prompts the user.
   - **Yes** → downloads the new JAR as a `.tmp` file, then hands off to `Updater.exe` to swap files and relaunches.
   - **No** → runs the existing JAR immediately.
5. If already up to date → runs the existing JAR immediately.
