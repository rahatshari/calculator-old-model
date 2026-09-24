import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;
const HOST = '0.0.0.0';

// Serve static assets from root directory
app.use(express.static(__dirname));

// Direct download for index.html
app.get('/download-html', (req, res) => {
  res.download(path.join(__dirname, 'index.html'), 'index.html');
});

// Direct download for all files (.tar.gz)
app.get('/download-all', (req, res) => {
  res.download(path.join(__dirname, 'app-source.tar.gz'), 'app-source.tar.gz');
});

// Fallback to index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, HOST, () => {
  console.log(`Server running at http://${HOST}:${PORT}`);
});
