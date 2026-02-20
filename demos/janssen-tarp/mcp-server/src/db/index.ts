// db/index.ts
import { Low } from 'lowdb';
import { JSONFile } from 'lowdb/node';
import { DatabaseSchema, defaultData } from './models.js';

const DB_FILE = process.env.DB_FILE || 'db.json';

class Database {
  private db: Low<DatabaseSchema>;

  constructor() {
    const adapter = new JSONFile<DatabaseSchema>(DB_FILE);
    this.db = new Low<DatabaseSchema>(adapter, defaultData);
  }

  async initialize(): Promise<void> {
    try {
      await this.db.read();
      this.db.data ||= defaultData;
      await this.db.write();
      console.log(`Database initialized at ${DB_FILE}`);
    } catch (error) {
      console.error(`Failed to initialize database at ${DB_FILE}:`, error);
      throw new Error(`Database initialization failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  get instance(): Low<DatabaseSchema> {
    return this.db;
  }
}

// Create singleton instance
export const database = new Database();