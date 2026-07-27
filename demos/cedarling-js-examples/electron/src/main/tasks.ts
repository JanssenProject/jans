export type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

let nextId = 3;

const initialTasks: Task[] = [
  { id: 'task-1', title: 'Buy groceries', completed: false, owner: 'bob' },
  {
    id: 'task-2',
    title: 'Schedule meeting with CEO',
    completed: false,
    owner: 'alice',
  },
];

const tasks: Task[] = [...initialTasks];

export function getAll(): Task[] {
  return tasks;
}

export function findById(id: string): Task | undefined {
  return tasks.find((t) => t.id === id);
}

export function create(title: string, owner: string): Task {
  const task: Task = {
    id: `task-${nextId++}`,
    title,
    completed: false,
    owner,
  };
  tasks.push(task);
  return task;
}

export function update(
  id: string,
  data: { title?: string; completed?: boolean },
): Task | null {
  const task = tasks.find((t) => t.id === id);
  if (!task) return null;
  if (data.title !== undefined) task.title = data.title;
  if (data.completed !== undefined) task.completed = data.completed;
  return task;
}

export function remove(id: string): boolean {
  const idx = tasks.findIndex((t) => t.id === id);
  if (idx === -1) return false;
  tasks.splice(idx, 1);
  return true;
}
