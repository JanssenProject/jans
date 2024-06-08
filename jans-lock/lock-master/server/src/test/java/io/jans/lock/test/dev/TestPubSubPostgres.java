/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.test.dev;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class TestPubSubPostgres {

	public static void main(String args[]) throws Exception {
		List<ListenerPostgres> pubSubList = new ArrayList<>();

		Class.forName("org.postgresql.Driver");
		String url = "jdbc:postgresql://localhost:5432/postgres";

		// Create two distinct connections, one for the notifier
		// and another for the listener to show the communication
		// works across connections although this example would
		// work fine with just one connection.


		// Create two threads, one to issue notifications and
		// the other to receive them.

		for (int i = 1; i <= 3; i++) {
			Connection lConn = DriverManager.getConnection(url, "postgres", "Secret1!");
			ListenerPostgres listener = new ListenerPostgres(lConn, i);
			listener.start();
			pubSubList.add(listener);
		}

		Thread.sleep(1000);
		Connection nConn = DriverManager.getConnection(url, "postgres", "Secret1!");
        ExecutorService executorService = Executors.newFixedThreadPool(10, daemonThreadFactory());
        for (int i = 0; i < 10; i++) {
        	executorService.execute(new NotifierPostgres(nConn));
        }

		
		Thread.sleep(10*1000);
		Random random = new Random();
		for (int i = 1; i <= 1; i++) {
			int idx = random.nextInt(pubSubList.size());
			ListenerPostgres listenerPostgres = pubSubList.get(idx);
			pubSubList.remove(idx);

			listenerPostgres.unsubscribe();
			Thread.sleep(2*1000);
		}
	
	}


	public static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

}

class ListenerPostgres extends Thread {
	private Connection conn;
	private org.postgresql.PGConnection pgconn;
	private int idx;
	private boolean closed = false;

	ListenerPostgres(Connection conn, int idx) throws SQLException {
		this.conn = conn;
		this.idx = idx;
		this.pgconn = conn.unwrap(org.postgresql.PGConnection.class);
		Statement stmt = conn.createStatement();
		stmt.execute("LISTEN mymessage test2");
		stmt.close();
	}

	public void unsubscribe() {
		try {
			this.closed  = true;
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		long total = 0;
		try {
			while (!closed) {
				org.postgresql.PGNotification notifications[] = pgconn.getNotifications(500);

				// If this thread is the only one that uses the connection, a timeout can be
				// used to
				// receive notifications immediately:
				// org.postgresql.PGNotification notifications[] =
				// pgconn.getNotifications(10000);

				total += notifications.length;
				System.out.printf("%d:\t %d\t: Got notification '%d' count\n", idx, total, notifications.length);
				if (notifications != null) {
					for (int i = 0; i < notifications.length; i++) {
//						System.out.printf("Got notification: '%s' with parameter '%s'\n", notifications[i].getName(), notifications[i].getParameter());
					}
				}

				// wait a while before checking again for new
				// notifications

				Thread.sleep(200);
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}
}

class NotifierPostgres extends Thread {
	private Connection conn;
	private static long total = 0;

	public NotifierPostgres(Connection conn) {
		this.conn = conn;
	}

	public void run() {
		while (true) {
			try {
				Statement stmt = conn.createStatement();
				stmt.execute("NOTIFY mymessage, '" + Base64.getEncoder().encodeToString(String.valueOf(new Random().nextInt(31*6)).getBytes()) + "'");
				stmt.close();
				total++;
				
				if (total % 1000 == 0) {
					System.out.printf("-:\t %d\t: Send notifications count\n", total);
				}
//				Thread.sleep(200);
			} catch (SQLException sqle) {
				sqle.printStackTrace();
//			} catch (InterruptedException ie) {
//				ie.printStackTrace();
			}
		}
	}


}
