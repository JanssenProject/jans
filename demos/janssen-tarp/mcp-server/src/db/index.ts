// db/index.ts
import { Low } from 'lowdb';
import { JSONFile } from 'lowdb/node';
import { DatabaseSchema, defaultData } from './models.js';

const DB_FILE = 'db.json';

class Database {
  private db: Low<DatabaseSchema>;

  constructor() {
    const adapter = new JSONFile<DatabaseSchema>(DB_FILE);
    this.db = new Low<DatabaseSchema>(adapter, defaultData);
  }

  async initialize(): Promise<void> {
    await this.db.read();
    this.db.data ||= defaultData;
    await this.db.write();
    console.log(`Database initialized at ${DB_FILE}`);
  }

  get instance(): Low<DatabaseSchema> {
    return this.db;
  }
}

// Create singleton instance
export const database = new Database();