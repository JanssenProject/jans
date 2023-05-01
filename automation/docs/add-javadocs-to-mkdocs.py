# Description: This script will scan the javadocs folder and append all the files to mkdocs.yml file
from pathlib import Path
import sys
import yaml


# Function to recursively scan the Javadocs folder
# and return a list of directories names and file paths in the folder
def get_dir_list(path):
    dir_list = []
    for p in Path(path).iterdir():
        if p.is_dir():
            dir_list.append({p.name: get_dir_list(p)})
        else:
            try:
                index = dir_list.index(p.name)
                dir_list[index] = {p.name.split(".html")[0]: str(p).split("/jans/docs/")[1]}
            except ValueError:
                dir_list.append({p.name.split(".html")[0]: str(p).split("/jans/docs/")[1]})
    return dir_list


# function with list input that loops through and appends the headers and files to mkdocs.yml file
def append_to_mkdocs(dir_list):
    # open mkdocs yaml file
    with open("mkdocs.yml", 'r') as f:
        mkdocs = yaml.safe_load(f)
        dev_index = None
        java_docs_index = None
        for i in range(len(mkdocs['nav'])):
            if "Administration" in mkdocs['nav'][i]:
                dev_index = i
                for j in range(len(mkdocs['nav'][i]['Administration'])):
                    if "Reference Guide" in mkdocs['nav'][i]['Administration'][j]:
                        ref_guide_index = j
                        for k in range(len(mkdocs['nav'][i]['Administration'][j]['Reference Guide'])):
                            if "Javadocs / OpenAPI" in mkdocs['nav'][i]['Administration'][j]['Reference Guide'][k]:
                                java_docs_index = k
                                break
        mkdocs['nav'][dev_index]['Administration'][ref_guide_index]['Reference Guide'][java_docs_index][
            'Javadocs / OpenAPI'] = dir_list
    # write to mkdocs yaml file
    with open("mkdocs.yml", 'w') as f:
        yaml.dump(mkdocs, f)


def main():
    path = Path("./docs/admin/reference/javadocs").resolve()
    dir_list = get_dir_list(path)
    print("scan completed")
    append_to_mkdocs(dir_list)
    print("mkdocs.yml updated")


if __name__ == "__main__":
    main()
