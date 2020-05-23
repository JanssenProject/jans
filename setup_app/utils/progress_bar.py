import sys
import time

class ProgressBar:

    def __init__(self, cols, queue=None, max_steps=33):
        self.n = 0
        self.queue = queue
        self.max_steps = max_steps
        self.tty_columns = int(cols)
        self.max_text = self.tty_columns-48

    def complete(self, msg):
        self.n = self.max_steps
        self.progress(msg, False)

    def progress(self, ptype, msg, incr=True):
        if incr and self.n < self.max_steps:
            self.n +=1

        time.sleep(0.2)

        if self.queue:
            if msg == 'Completed':
                self.queue.put((COMPLETED, ptype, msg))
            else:
                self.queue.put((self.n, ptype, msg))
        else:
            ft = '#' * self.n
            ft = ft.ljust(self.max_steps)
            msg = msg.ljust(40)
            msg = msg[:self.max_text]

            sys.stdout.write("\rInstalling [{0}] {1}".format(ft, msg))
            sys.stdout.flush()
