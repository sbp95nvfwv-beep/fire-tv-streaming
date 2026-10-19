import crypto from 'node:crypto';
import fs from 'node:fs/promises';
import fsSync from 'node:fs';
import path from 'node:path';
import express from 'express';
import cors from 'cors';
import multer from 'multer';

const port = Number(process.env.PORT || 8080);
const mediaDir = path.resolve(process.env.MEDIA_DIR || './data/media');
const catalogFile = path.resolve(process.env.CATALOG_FILE || './data/catalog.json');
const adminToken = process.env.ADMIN_TOKEN;
if (!adminToken) throw new Error('ADMIN_TOKEN is required. Copy .env.example to .env and set a secret.');

await fs.mkdir(mediaDir, { recursive: true });
await fs.mkdir(path.dirname(catalogFile), { recursive: true });
try { await fs.access(catalogFile); } catch { await fs.writeFile(catalogFile, '[]\n'); }

const readCatalog = async () => JSON.parse(await fs.readFile(catalogFile, 'utf8'));
const writeCatalog = async catalog => fs.writeFile(catalogFile, `${JSON.stringify(catalog, null, 2)}\n`);
const safeName = value => value.replace(/[^a-zA-Z0-9._-]/g, '_');
const requireAdmin = (req, res, next) => req.get('authorization') === `Bearer ${adminToken}` ? next() : res.status(401).json({ error: 'Unauthorized' });

const storage = multer.diskStorage({
  destination: (_req, _file, done) => done(null, mediaDir),
  filename: (_req, file, done) => done(null, `${crypto.randomUUID()}-${safeName(file.originalname)}`)
});
const upload = multer({ storage, limits: { fileSize: 20 * 1024 * 1024 * 1024 }, fileFilter: (_req, file, done) => done(null, file.mimetype.startsWith('video/')) });

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.resolve('web')));

app.get('/api/health', (_req, res) => res.json({ ok: true }));
app.get('/api/library', async (_req, res, next) => {
  try { res.json(await readCatalog()); } catch (error) { next(error); }
});
app.post('/api/movies', requireAdmin, upload.single('video'), async (req, res, next) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'A video file is required.' });
    const movie = { id: crypto.randomUUID(), title: String(req.body.title || path.parse(req.file.originalname).name), description: String(req.body.description || ''), filename: req.file.filename, mimeType: req.file.mimetype || 'video/mp4', createdAt: new Date().toISOString() };
    const catalog = await readCatalog(); catalog.unshift(movie); await writeCatalog(catalog);
    res.status(201).json(movie);
  } catch (error) { next(error); }
});
app.delete('/api/movies/:id', requireAdmin, async (req, res, next) => {
  try {
    const catalog = await readCatalog(); const movie = catalog.find(item => item.id === req.params.id);
    if (!movie) return res.sendStatus(404);
    await fs.rm(path.join(mediaDir, movie.filename), { force: true });
    await writeCatalog(catalog.filter(item => item.id !== movie.id)); res.sendStatus(204);
  } catch (error) { next(error); }
});
app.get('/api/movies/:id/stream', async (req, res, next) => {
  try {
    const movie = (await readCatalog()).find(item => item.id === req.params.id);
    if (!movie) return res.sendStatus(404);
    const file = path.join(mediaDir, movie.filename); const stat = await fs.stat(file); const range = req.headers.range;
    res.setHeader('Accept-Ranges', 'bytes'); res.setHeader('Content-Type', movie.mimeType);
    if (!range) { res.setHeader('Content-Length', stat.size); return fsSync.createReadStream(file).pipe(res); }
    const [startText, endText] = range.replace('bytes=', '').split('-'); const start = Number(startText); const end = endText ? Number(endText) : stat.size - 1;
    if (start >= stat.size || end >= stat.size) return res.status(416).set('Content-Range', `bytes */${stat.size}`).end();
    res.status(206).set({ 'Content-Range': `bytes ${start}-${end}/${stat.size}`, 'Content-Length': end - start + 1 });
    fsSync.createReadStream(file, { start, end }).pipe(res);
  } catch (error) { next(error); }
});
app.use((error, _req, res, _next) => { console.error(error); res.status(500).json({ error: 'Server error' }); });
app.listen(port, '0.0.0.0', () => console.log(`Media server at http://0.0.0.0:${port}`));
