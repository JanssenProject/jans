import express from 'express';
import cors from 'cors';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { init, authorizeMiddleware } from './cedarling/index.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const app = express();
app.use(cors());
app.use(express.json());

const tasks = [
  { id: 'task-1', title: 'Buy groceries', completed: false, owner: 'bob' },
  { id: 'task-2', title: 'Schedule meeting with CEO', completed: false, owner: 'alice' },
];

const policyStorePath = path.resolve(__dirname, '../../common/policy-store.json');
const policyStore = JSON.parse(fs.readFileSync(policyStorePath, 'utf8'));

const cedarling = await init();
const authorize = authorizeMiddleware(cedarling, tasks);

const testConfigPath = path.resolve(__dirname, '../../common/test-config.json');
const testConfig = JSON.parse(fs.readFileSync(testConfigPath, 'utf8'));

app.get('/config/policy-store', (_req, res) => {
  res.json(policyStore);
});

app.get('/config/test-config', (_req, res) => {
  res.json(testConfig);
});

app.get('/tasks', authorize('ViewTask'), (req, res) => {
  res.json(tasks);
});

app.post('/tasks', authorize('CreateTask'), (req, res) => {
  const { title } = req.body;
  const newTask = {
    id: `task-${Date.now()}`,
    title,
    completed: false,
    owner: req.userId,
  };
  tasks.push(newTask);
  res.status(201).json(newTask);
});

app.put('/tasks/:id', authorize('UpdateTask', (req) => req.params.id), (req, res) => {
  const { id } = req.params;
  const { completed, title } = req.body;
  const task = tasks.find((t) => t.id === id);
  if (!task) {
    return res.status(404).json({ error: 'Task not found' });
  }
  if (completed !== undefined) task.completed = completed;
  if (title !== undefined) task.title = title;
  res.json(task);
});

app.delete('/tasks/:id', authorize('DeleteTask', (req) => req.params.id), (req, res) => {
  const { id } = req.params;
  const index = tasks.findIndex((t) => t.id === id);
  if (index === -1) {
    return res.status(404).json({ error: 'Task not found' });
  }
  tasks.splice(index, 1);
  res.status(204).send();
});

const PORT = 8080;
app.listen(PORT, () => {
  console.log(`TaskApp Express Backend running on http://localhost:${PORT}`);
});
