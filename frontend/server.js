const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 5173;

const MIME = {
  '.html': 'text/html',
  '.css':  'text/css',
  '.js':   'application/javascript',
  '.json': 'application/json',
  '.png':  'image/png',
  '.ico':  'image/x-icon',
  '.svg':  'image/svg+xml',
};

const server = http.createServer((req, res) => {
  let urlPath = req.url.split('?')[0];
  if (urlPath === '/' || !urlPath.includes('.')) urlPath = '/index.html';

  const filePath = path.join(__dirname, urlPath);
  const ext = path.extname(filePath);
  const contentType = MIME[ext] || 'text/plain';

  fs.readFile(filePath, (err, data) => {
    if (err) {
      // SPA fallback
      fs.readFile(path.join(__dirname, 'index.html'), (e2, d2) => {
        res.writeHead(e2 ? 404 : 200, { 'Content-Type': 'text/html' });
        res.end(e2 ? '404 Not Found' : d2);
      });
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(data);
    }
  });
});

server.listen(PORT, () => {
  console.log(`\n  ResumeIQ Frontend running at http://localhost:${PORT}\n`);
});
