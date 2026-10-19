# Linux-Mediathek starten

Dieses Paket stellt den Server für die Fire-TV-App bereit. Es ist für ein privates Heimnetz gedacht.

1. Python 3 installieren (bei den meisten Linux-Distributionen bereits vorhanden).
2. Das Paket entpacken und im Ordner `chmod +x start-linux.sh` ausführen.
3. `./start-linux.sh` starten. Beim ersten Start wird ein Admin-Token in `.env` erzeugt, der Medienserver gestartet und die Upload-Seite im Browser geöffnet.
4. Dort das Token aus `.env` eintragen und Videos hochladen.
5. In der Fire-TV-App die LAN-IP des Linux-PCs eintragen, z. B. `http://192.168.178.20:8080`.

Die Fire-TV-App lädt die Bibliothek bei jedem Start neu. Neue Uploads erscheinen so automatisch, ohne dass du die APK neu installieren musst. PC und Fire TV müssen im gleichen WLAN/LAN sein.

Öffne den Server nicht direkt ins Internet. Für Fernzugriff wären HTTPS, Benutzerkonten und ein Reverse Proxy nötig.
