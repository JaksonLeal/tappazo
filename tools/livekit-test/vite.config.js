import { readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { defineConfig } from 'vite';

const certificateDirectory = join(
  process.env.LOCALAPPDATA ?? join(homedir(), 'AppData', 'Local'),
  'Tappazo',
  'livekit-test',
);

let pfx;
let passphrase;

try {
  pfx = readFileSync(join(certificateDirectory, 'server.p12'));
  passphrase = readFileSync(join(certificateDirectory, 'server-password.txt'), 'utf8').trim();
} catch {}

export default defineConfig(({ command }) => {
  if (command === 'serve' && (!pfx || !passphrase)) {
    throw new Error(
      'LAN certificate not found. Run .\\tools\\livekit-test\\setup-lan.ps1 first.',
    );
  }

  return {
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      https: pfx && passphrase ? { pfx, passphrase } : undefined,
      proxy: {
        '/livekit': {
          target: 'ws://127.0.0.1:7880',
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/livekit/, ''),
        },
      },
    },
  };
});
