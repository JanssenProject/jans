"""
 License terms and conditions for Janssen Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
"""
import subprocess
import shlex
import logging
import json
import errno
import shutil
from pathlib import Path


def update_json_file(settings, file):
    """Write settings out to a json file
    :param settings:
    """
    with open(Path(file), 'w+') as file:
        json.dump(settings, file, indent=2)


def exec_cmd(cmd, output_file=None):
    """
    Execute command cmd
    :param cmd:
    :param output_file:
    :return:
    """
    args = shlex.split(cmd)
    popen = subprocess.Popen(args,
                             stdin=subprocess.PIPE,
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)
    stdout, stderr = popen.communicate()
    retcode = popen.returncode
    if stdout and output_file:
        with open(output_file, "w+") as file:
            file.write(str(stdout, "utf-8"))

    if retcode != 0:
        logger.error(str(stderr, "utf-8"))
    return stdout, stderr, retcode


def get_logger(name):
    """
    Set logger configs with name.
    :param name:
    :return:
    """
    log_format = '%(asctime)s - %(name)8s - %(levelname)5s - %(message)s'
    logging.basicConfig(level=logging.INFO,
                        format=log_format,
                        filename='setup.log',
                        filemode='w')
    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    console.setFormatter(logging.Formatter(log_format))
    logging.getLogger(name).addHandler(console)
    return logging.getLogger(name)


logger = get_logger("cn-helpers   ")


def copy(src, dest):
    """
    Copy from source to destination
    :param src:
    :param dest:
    """
    try:
        shutil.copytree(src, dest)
    except OSError as e:
        # If the error was caused because the source wasn't a directory
        if e.errno == errno.ENOTDIR:
            shutil.copy(src, dest)
        else:
            logger.error('Directory not copied. Error: {}'.format(e))
