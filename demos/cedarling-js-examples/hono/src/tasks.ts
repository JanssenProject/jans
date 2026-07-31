export interface Task {
  id: string;
  title: string;
  completed: boolean;
  owner: string;
}

const DEFAULT_TASKS: readonly Task[] = [
  { id: "task-1", title: "Buy groceries", completed: false, owner: "bob" },
  { id: "task-2", title: "Schedule meeting with CEO", completed: false, owner: "alice" },
];

export function createTaskStore(initialTasks = DEFAULT_TASKS) {
  const tasks = initialTasks.map((task) => ({ ...task }));
  return {
    all: () => tasks,
    find: (id: string) => tasks.find((task) => task.id === id),
    create(title: string, owner: string) {
      const task = { id: `task-${Date.now()}`, title, completed: false, owner };
      tasks.push(task);
      return task;
    },
    update(id: string, changes: Partial<Pick<Task, "title" | "completed">>) {
      const task = tasks.find((candidate) => candidate.id === id);
      if (!task) return undefined;
      if (changes.title !== undefined) task.title = changes.title;
      if (changes.completed !== undefined) task.completed = changes.completed;
      return task;
    },
    remove(id: string) {
      const index = tasks.findIndex((task) => task.id === id);
      if (index < 0) return false;
      tasks.splice(index, 1);
      return true;
    },
  };
}

export type TaskStore = ReturnType<typeof createTaskStore>;
