const http = require('http');
const fs = require('fs');
const path = require('path');

const apkPath = 'D:\\Desktop\\KaraokeTV.apk';

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  if (req.url === '/' || req.url === '/KaraokeTV.apk' || req.url === '/apk') {
    if (fs.existsSync(apkPath)) {
      const stat = fs.statSync(apkPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="KaraokeTV.apk"'
      });
      fs.createReadStream(apkPath).pipe(res);
    } else {
      res.writeHead(404);
      res.end('APK not found');
    }
  } else {
    res.writeHead(404);
    res.end('Not found');
  }
});

server.listen(8080, '0.0.0.0', () => {
  console.log('APK Server running on http://0.0.0.0:8080/KaraokeTV.apk');
});
