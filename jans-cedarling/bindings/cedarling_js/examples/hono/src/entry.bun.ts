import app from './app';

const PORT = parseInt(process.env.PORT || '3001', 10);

Bun.serve({
  port: PORT,
  fetch: app.fetch,
});
