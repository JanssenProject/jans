"""
pygluu.kubernetes.common
~~~~~~~~~~~~~~~~~~~~~~~~

License terms and conditions for Gluu Cloud Native Edition:
https://www.apache.org/licenses/LICENSE-2.0
"""
import errno
import subprocess
import shlex
import logging
import json
import shutil
import os
import string
import random
import re
from getpass import getpass
from pathlib import Path


def update_settings_json_file(settings):
    """Write settings out to a json file

    :param settings:
    """
    with open(Path('./settings.json'), 'w+') as file:
        json.dump(settings, file, indent=2)


def exec_cmd(cmd, output_file=None, silent=False):
    """Execute command cmd

    :param cmd:
    :param output_file:
    :param silent:
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
    else:
        logger.info(str(stdout, "utf-8"))
    if retcode != 0 and not silent:
        logger.error(str(stderr, "utf-8"))
    return stdout, stderr, retcode


def get_logger(name):
    """Set logger configs with name.

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


def copy_templates():
    """Copy templates folder. /pygluu/kubernetes/templates to working dir.
    """
    entries = Path(
        os.path.join(os.path.dirname(__file__), "templates")
    )
    curdir = os.getcwd()
    for entry in entries.iterdir():
        dst = os.path.join(curdir, entry.name)
        if os.path.exists(dst):
            continue
        copy(entry, dst)


def check_microk8s_kube_config_file():
    """Copy microk8s kuber config to ~/.kube/config
    """
    kube_config_file_location = Path(os.path.expanduser("~/.kube/config"))

    if not kube_config_file_location.exists():
        kube_dir = os.path.dirname(kube_config_file_location)

        if not os.path.exists(kube_dir):
            os.makedirs(kube_dir)

        try:
            shutil.copy(Path("/var/snap/microk8s/current/credentials/client.config"), kube_config_file_location)
        except FileNotFoundError:
            logger.error("No Kubernetes config file found at ~/.kube/config")


def get_supported_versions():
    """Get Gluu versions from gluu_versions.json

    return:
    """
    versions = {}
    version_number = 0
    dev_version = ""

    filename = Path("./gluu_versions.json")
    try:
        with open(filename) as f:
            versions = json.load(f)
        logger.info("Currently supported versions are : ")
        for k, v in versions.items():
            if "_dev" in k:
                logger.info("Development version : {}".format(k))
                dev_version = k
            else:
                logger.info("Stable version : {}".format(k))
                if float(k) > version_number:
                    version_number = float(k)
    except FileNotFoundError:
        pass
    finally:
        if not version_number:
            # No stable version exists
            version_number = dev_version

    version_number = str(version_number)
    return versions, version_number


def generate_password(length=6):
    """Returns randomly generated password

    :param length: Length of password
    :return:
    """
    chars = string.ascii_letters + string.digits + string.punctuation + string.punctuation
    chars = chars.replace('"', '')
    chars = chars.replace("'", "")
    chars = chars.replace("$", "")
    chars = chars.replace("/", "")
    chars = chars.replace("\\", "")
    chars = chars.replace("!", "")

    while True:
        password = ''.join(random.choice(chars) for _ in range(length))
        regex_bool = re.match('^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*\W)[a-zA-Z0-9\S]{6,}$', password)  # noqa: W605
        if regex_bool:
            break

    return password


def prompt_password(password, length=6):
    """Prompt password and password confirmation

    :param password: string for the prompt name
    :param length: Length of password
    :return:
    """
    while True:
        random_password = "" if password == "Redis" else generate_password(length)
        string_random_password = '' if not random_password else random_password[:1] + "***" + random_password[4:]
        pw_prompt = getpass(prompt='{} password [{}]: '.format(password, string_random_password), stream=None)
        regex_bool = True
        if not pw_prompt:
            pw_prompt = random_password
            confirm_pw_prompt = random_password
        else:
            confirm_pw_prompt = getpass(prompt='Confirm password: ', stream=None)
            if password != "Redis":
                regex_bool = re.match('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*\\W)[a-zA-Z0-9\\S]{6,}$',
                                      pw_prompt)  # noqa: W605

        if confirm_pw_prompt != pw_prompt:
            logger.error("Passwords do not match")
        elif not regex_bool:
            logger.error("Password does not meet requirements. The password must contain one digit, one uppercase"
                         " letter, one lower case letter and one symbol")
        else:
            logger.info("Success! {} password was set.".format(password))
            return pw_prompt


def copy(src, dest):
    """Copy from source to destination

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


logger = get_logger("gluu-common        ")
