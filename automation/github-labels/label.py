import json
from pathlib import Path
import subprocess
import shlex
import sys

# ./automation/github-labels
label_schema_json = Path("./automation/github-labels/labels-schema.json")


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
        print(str(stdout, "utf-8"))
    if retcode != 0 and not silent:
        print(str(stderr, "utf-8"))
    return stdout, stderr, retcode


def load_labels_schema():
    json_schema = {}
    with open(label_schema_json, "r") as schema_file:
        json_schema = json.load(schema_file)
    return json_schema


def create_labels():
    json_schema = load_labels_schema()
    stdout, stderr, retcode = exec_cmd("gh label list", silent=True)
    existing_labels = str(stdout, "utf-8")
    for k, v in json_schema.items():
        if k in existing_labels:
            print(f"Label {k} already exists! Skipping...")
        else:
            try:
                stdout, stderr, retcode = exec_cmd(
                    f"gh label create {k} --description '{v['description']}' --color '{v['color']}'")
                if "Label.name already exists" in str(stderr, "utf-8"):
                    print(f"Label {k} already exists! Skipping...")
            except Exception as e:
                print(f"Couldn't create label {k} because {e}")


def auto_label(operation="pr", issue_or_pr_number=None, paths=None, branch=None, title=None):
    labels = []
    json_schema = load_labels_schema()
    # Loop over all labels
    for k, v in json_schema.items():
        # Check title label. Label based on conventional commit title
        if title:
            for title_prefix in v["auto-label"]["title-prefixes"]:
                try:
                    if title_prefix in title.split(':')[0]:
                        print(f"Detected label from title. Adding label {k}")
                        labels.append(k)
                except Exception as e:
                    print(e)

        # Check branch label
        if branch:
            if branch == v["auto-label"]["branch"]:
                print(f"Detected label from branch. Adding label {k}")
                labels.append(k)

        if paths:
            # Check if comp-*, area-* labels need to be added
            allpaths = paths.split(",")
            for path in allpaths:
                # Check file path for direct hit
                if path in v["auto-label"]["paths"]:
                    print(f"Detected label from exact path. Adding label {k}")
                    labels.append(k)
                else:
                    # Label all .md with area-documentation
                    if k == "area-documentation" and ".md" in path:
                        print(f"Detected a markdown file in the path. Adding label {k}")
                        labels.append(k)
                    else:
                        # Check if main directories need exist in paths
                        try:
                            for i in range(len(path.split("/"))):
                                # Check main directories i.e docs .github .github/workflows
                                if path.split("/")[i] in v["auto-label"]["paths"]:
                                    print(f"Detected label from folder. Adding label {k}")
                                    labels.append(k)
                        except IndexError:
                            print("Got an index issue!")

    # removes duplicate labels and joins them by comma
    string_of_labels = ",".join(list(dict.fromkeys(labels)))
    try:
        print(f"gh {operation} edit {issue_or_pr_number} --add-label '{string_of_labels}'")
        exec_cmd(f"gh {operation} edit {issue_or_pr_number} --add-label '{string_of_labels}'")
    except Exception as e:
        print(f"Couldn't add the label to the PR {issue_or_pr_number} because {e}")


def main():
    issue_or_pr_number = sys.argv[1]
    changed_files = None if sys.argv[2] == "NONE" else sys.argv[2]
    branch = None if sys.argv[3] == "NONE" else sys.argv[3]
    operation = sys.argv[4]
    title = sys.argv[5]

    print(f"Starting to add labels for {operation} {issue_or_pr_number}")
    print(f"Detected the following changed files {changed_files}")
    print(f"Detected the following head branch {branch}")
    print(f"Detected the following title {title}")
    create_labels()
    auto_label(operation, issue_or_pr_number, changed_files, branch, title)


if __name__ == "__main__":
    main()
