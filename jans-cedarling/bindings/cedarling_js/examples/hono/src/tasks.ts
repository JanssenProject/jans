export type Task = {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
};

const tasks: Task[] = [
  { id: 'task-1', title: 'Buy groceries', completed: false, owner: 'bob' },
  { id: 'task-2', title: 'Schedule meeting with CEO', completed: false, owner: 'alice' },
];

export function getAll(): Task[] {
  return tasks;
}

export function findById(id: string): Task | undefined {
  return tasks.find((t) => t.id === id);
}

export function create(title: string, owner: string): Task {
  const newTask: Task = {
    id: `task-${Date.now()}`,
    title,
    completed: false,
    owner,
  };
  tasks.push(newTask);
  return newTask;
}

export function update(id: string, changes: Partial<Pick<Task, 'title' | 'completed'>>): Task | null {
  const task = tasks.find((t) => t.id === id);
  if (!task) return null;
  if (changes.title !== undefined) task.title = changes.title;
  if (changes.completed !== undefined) task.completed = changes.completed;
  return task;
}

export function remove(id: string): boolean {
  const index = tasks.findIndex((t) => t.id === id);
  if (index === -1) return false;
  tasks.splice(index, 1);
  return true;
}
