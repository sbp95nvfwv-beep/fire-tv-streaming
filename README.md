# Fire TV Streaming

Private Mediathek: Videos im Browser hochladen und im Heimnetz über eine Fire-TV-App ansehen. Verwende nur Inhalte, für die du die nötigen Rechte hast.

## Start auf dem PC

1. `cp .env.example .env` und einen langen zufälligen Wert für `ADMIN_TOKEN` setzen.
2. `./start.sh` startet den Server und öffnet die Verwaltungsseite im Browser. Alternativ: `docker compose up --build -d`.
3. Im Browser `http://localhost:8080` öffnen, Token eingeben und Videos hochladen.

Die App nutzt progressive HTTP-Streams. Für zuverlässige Wiedergabe sollten Uploads H.264/AAC in MP4 sein. Eine spätere Erweiterung kann FFmpeg/HLS-Transkodierung hinzufügen.

## Fire TV

1. Android Studio öffnen: Ordner `android`.
2. Debug-APK erzeugen: `./gradlew :app:assembleDebug` (oder über Android Studio).
3. APK per ADB oder Downloader auf dem Fire TV installieren.
4. Beim ersten Start die LAN-Adresse des PCs eingeben, etwa `http://192.168.178.25:8080`.

PC und Fire TV müssen im gleichen WLAN sein. Die App lädt die Mediathek bei jedem Öffnen neu; neue Uploads erscheinen damit ohne neues APK.

## Linux-PC als Medienserver

Für Linux enthält das Projekt `start-linux.sh` und `README-LINUX.md`. Das Skript erzeugt beim ersten Start einen Admin-Token, startet den Docker-Server und öffnet die Upload-Seite. Die Fire-TV-App verbindet sich mit der LAN-Adresse dieses PCs.

## APK und GitHub Release

GitHub Actions erstellt bei jedem Push eine Debug-APK als Workflow-Artefakt. Sobald ein Tag wie `v0.1.0` nach GitHub gepusht wird, erstellt der Workflow ein GitHub Release und hängt `app-debug.apk` an. Debug-APKs sind für private Tests geeignet; für eine Verteilung außerhalb des eigenen Kreises sollte die App mit einem eigenen Android-Keystore als Release signiert werden.

Version `v0.1.4` verwendet für Fire OS eine native Android-Activity und ein passendes System-Theme. Das beseitigt die verbleibende Theme-Abhängigkeit beim App-Start.

## Sicherheit

Der Upload-Endpunkt verlangt den `ADMIN_TOKEN`; die Fire-TV-Wiedergabe ist im Heimnetz bewusst ohne Token. Setze den Dienst nicht ohne HTTPS, Benutzerkonten und einen Reverse Proxy direkt ins Internet.
