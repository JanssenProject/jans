export type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

const globalForTasks = globalThis as typeof globalThis & {
  taskAppTasks?: Task[];
};

function getStore(): Task[] {
  return globalForTasks.taskAppTasks ??= [
    { id: 'task-1', title: 'Buy groceries', completed: false, owner: 'bob' },
    { id: 'task-2', title: 'Schedule meeting with CEO', completed: false, owner: 'alice' },
  ];
}

export function getAll(): Task[] {
  return getStore();
}

export function findById(id: string): Task | undefined {
  return getStore().find((t) => t.id === id);
}

export function create(title: string, owner: string): Task {
  const task: Task = { id: `task-${Date.now()}`, title, completed: false, owner };
  getStore().push(task);
  return task;
}

export function update(id: string, changes: Partial<Pick<Task, 'title' | 'completed'>>): Task | null {
  const tasks = getStore();
  const task = tasks.find((t) => t.id === id);
  if (!task) return null;
  Object.assign(task, changes);
  return task;
}

export function remove(id: string): boolean {
  const tasks = getStore();
  const idx = tasks.findIndex((t) => t.id === id);
  if (idx === -1) return false;
  tasks.splice(idx, 1);
  return true;
}
