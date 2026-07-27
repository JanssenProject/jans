import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { getAll, findById, create, update, remove } from './tasks';
import { authorizeAction, buildResource } from './cedarling/authorize';

const app = new Hono();

app.use('/*', cors());

app.get('/config/policy-store', async (c) => {
  const res = await fetch('http://localhost:9090/config/policy-store');
  const data = await res.json();
  return c.json(data);
});

app.get('/config/test-config', async (c) => {
  const res = await fetch('http://localhost:9090/config/test-config');
  const data = await res.json();
  return c.json(data);
});

app.get('/tasks', async (c) => {
  const userId = c.req.header('x-user-id') || 'bob';
  const authHeader = c.req.header('authorization');
  const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : undefined;

  const authorized = await authorizeAction('ViewTask', userId, buildResource(null), token);
  if (!authorized.allowed) return c.json({ error: 'Forbidden' }, 403);

  return c.json(getAll());
});

app.post('/tasks', async (c) => {
  const userId = c.req.header('x-user-id') || 'bob';
  const authHeader = c.req.header('authorization');
  const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : undefined;
  const body = await c.req.json<{ title: string }>();

  const authorized = await authorizeAction('CreateTask', userId, buildResource(null, body.title), token);
  if (!authorized.allowed) return c.json({ error: 'Forbidden' }, 403);

  const task = create(body.title, userId);
  return c.json(task, 201);
});

app.put('/tasks/:id', async (c) => {
  const id = c.req.param('id');
  const userId = c.req.header('x-user-id') || 'bob';
  const authHeader = c.req.header('authorization');
  const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : undefined;
  const body = await c.req.json<{ title?: string; completed?: boolean }>();

  const task = findById(id);
  if (!task) return c.json({ error: 'Task not found' }, 404);

  const authorized = await authorizeAction('UpdateTask', userId, buildResource(task), token);
  if (!authorized.allowed) return c.json({ error: 'Forbidden' }, 403);

  const updated = update(id, body);
  return c.json(updated);
});

app.delete('/tasks/:id', async (c) => {
  const id = c.req.param('id');
  const userId = c.req.header('x-user-id') || 'bob';
  const authHeader = c.req.header('authorization');
  const token = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : undefined;

  const task = findById(id);
  if (!task) return c.json({ error: 'Task not found' }, 404);

  const authorized = await authorizeAction('DeleteTask', userId, buildResource(task), token);
  if (!authorized.allowed) return c.json({ error: 'Forbidden' }, 403);

  remove(id);
  return c.body(null, 204);
});

export default app;
