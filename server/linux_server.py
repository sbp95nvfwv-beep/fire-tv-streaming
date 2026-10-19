#!/usr/bin/env python3
"""Dependency-free private media server for Linux PCs."""
import cgi, json, mimetypes, os, re, secrets, shutil, uuid
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"; MEDIA = DATA / "media"; CATALOG = DATA / "catalog.json"; WEB = ROOT / "web"
MEDIA.mkdir(parents=True, exist_ok=True); CATALOG.parent.mkdir(parents=True, exist_ok=True)
if not CATALOG.exists(): CATALOG.write_text("[]\n")
def env(name):
    values = {}
    file = ROOT / ".env"
    if file.exists():
        for line in file.read_text().splitlines():
            if "=" in line and not line.lstrip().startswith("#"): key, value = line.split("=", 1); values[key.strip()] = value.strip()
    return os.environ.get(name, values.get(name, ""))
TOKEN = env("ADMIN_TOKEN")
if not TOKEN: raise SystemExit("ADMIN_TOKEN fehlt. Starte start-linux.sh.")
def catalog(): return json.loads(CATALOG.read_text())
def save(items): CATALOG.write_text(json.dumps(items, indent=2) + "\n")
def send_json(handler, value, status=200):
    raw = json.dumps(value).encode(); handler.send_response(status); handler.send_header("Content-Type", "application/json"); handler.send_header("Content-Length", str(len(raw))); handler.end_headers(); handler.wfile.write(raw)
class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args): print(fmt % args)
    def admin(self): return self.headers.get("Authorization") == "Bearer " + TOKEN
    def do_GET(self):
        path = unquote(urlparse(self.path).path)
        if path == "/api/health": return send_json(self, {"ok": True})
        if path == "/api/library": return send_json(self, catalog())
        match = re.fullmatch(r"/api/movies/([\w-]+)/stream", path)
        if match: return self.stream(match.group(1))
        requested = WEB / ("index.html" if path == "/" else path.lstrip("/"))
        if not requested.is_file() or WEB not in requested.resolve().parents and requested.resolve() != WEB: self.send_error(404); return
        raw = requested.read_bytes(); self.send_response(200); self.send_header("Content-Type", mimetypes.guess_type(str(requested))[0] or "application/octet-stream"); self.send_header("Content-Length", str(len(raw))); self.end_headers(); self.wfile.write(raw)
    def stream(self, movie_id):
        movie = next((m for m in catalog() if m["id"] == movie_id), None)
        if not movie: self.send_error(404); return
        file = MEDIA / movie["filename"]
        if not file.is_file(): self.send_error(404); return
        size = file.stat().st_size; start, end = 0, size - 1; range_header = self.headers.get("Range", "")
        if range_header.startswith("bytes="):
            first, _, last = range_header[6:].partition("-"); start = int(first or 0); end = int(last) if last else end
            if start >= size or end >= size: self.send_response(416); self.send_header("Content-Range", f"bytes */{size}"); self.end_headers(); return
            self.send_response(206); self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        else: self.send_response(200)
        self.send_header("Content-Type", movie.get("mimeType") or "video/mp4"); self.send_header("Accept-Ranges", "bytes"); self.send_header("Content-Length", str(end-start+1)); self.end_headers()
        with file.open("rb") as handle: handle.seek(start); shutil.copyfileobj(handle, self.wfile, length=1024*1024)
    def do_POST(self):
        if self.path != "/api/movies": self.send_error(404); return
        if not self.admin(): return send_json(self, {"error": "Unauthorized"}, 401)
        try:
            form = cgi.FieldStorage(fp=self.rfile, headers=self.headers, environ={"REQUEST_METHOD":"POST", "CONTENT_TYPE":self.headers.get("Content-Type", "")})
            upload = form["video"]
            if not getattr(upload, "file", None): raise ValueError("Video fehlt")
            original = Path(upload.filename or "video.mp4").name; filename = uuid.uuid4().hex + "-" + re.sub(r"[^A-Za-z0-9._-]", "_", original)
            with (MEDIA / filename).open("wb") as output: shutil.copyfileobj(upload.file, output)
            item = {"id": str(uuid.uuid4()), "title": form.getfirst("title", Path(original).stem), "description": form.getfirst("description", ""), "filename": filename, "mimeType": upload.type or "video/mp4"}; items = catalog(); items.insert(0, item); save(items); send_json(self, item, 201)
        except Exception as error: send_json(self, {"error": str(error)}, 400)
    def do_DELETE(self):
        if not self.admin(): return send_json(self, {"error": "Unauthorized"}, 401)
        match = re.fullmatch(r"/api/movies/([\w-]+)", urlparse(self.path).path)
        if not match: self.send_error(404); return
        items = catalog(); item = next((m for m in items if m["id"] == match.group(1)), None)
        if not item: self.send_error(404); return
        (MEDIA / item["filename"]).unlink(missing_ok=True); save([m for m in items if m["id"] != item["id"]]); self.send_response(204); self.end_headers()
print("Media server on http://0.0.0.0:8080")
ThreadingHTTPServer(("0.0.0.0", int(env("PORT") or 8080)), Handler).serve_forever()
