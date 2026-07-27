import app from './app.ts';

const PORT = parseInt(Deno.env.get('PORT') || '3001', 10);

Deno.serve({ port: PORT }, app.fetch);
