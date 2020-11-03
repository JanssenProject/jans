"""
 License terms and conditions for Janssen Cloud Native Edition:
 https://www.apache.org/licenses/LICENSE-2.0
 Yaml parser
"""
from pathlib import Path
import contextlib
import os
from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap
from collections import OrderedDict, Mapping
from helpers import get_logger
logger = get_logger("cn-yaml-parser   ")


class Parser(dict):
    def __init__(self, filename, check_value=None, check_value_name=None, check_key='kind'):
        super().__init__()
        self.filename = Path(filename)
        self.yaml = YAML()
        self.yaml.preserve_quotes = True
        self.manifests_dict_list = []
        self.modify_dict = dict
        self.tmp_yaml_file = Path("./tmp.yaml")

        if check_value:
            if self.filename.exists():
                with open(filename) as file:
                    manifests_dicts = self.yaml.load_all(file)
                    for manifest in manifests_dicts:
                        try:
                            if manifest[check_key] == check_value:
                                if check_value_name:
                                    if manifest['metadata']['name'] == check_value_name:
                                        self.modify_dict = manifest
                                    else:
                                        self.manifests_dict_list.append(manifest)
                                else:
                                    self.modify_dict = manifest
                            else:
                                self.manifests_dict_list.append(manifest)
                        except KeyError:
                            # Key kind is not found so its the values.yaml for helm which only has one dict item
                            self.modify_dict = manifest
                with open(self.tmp_yaml_file, 'w') as file:
                    self.yaml.dump(self.modify_dict, file)

                with open(self.tmp_yaml_file) as f:
                    super(Parser, self).update(self.yaml.load(f) or {})

    @property
    def return_manifests_dict(self):
        if self.filename.exists():
            with open(self.filename) as file:
                manifests_dicts = self.yaml.load_all(file)
                for manifest in manifests_dicts:
                    self.manifests_dict_list.append(manifest)

            return self.manifests_dict_list

    def __setitem__(self, key, value):
        super(Parser, self).__setitem__(key, value)

    def dump_it(self):
        d = self.analyze_ordered_dict_object(self)
        final_manifest_dict_list = self.manifests_dict_list + [d]
        with open(self.filename, "w+") as f:
            self.yaml.dump_all(final_manifest_dict_list, f)
        with contextlib.suppress(FileNotFoundError):
            os.remove(self.tmp_yaml_file)

    def analyze_ordered_dict_object(self, data):
        if isinstance(data, OrderedDict) or isinstance(data, dict):
            commented_map = CommentedMap()
            for k, v in data.items():
                commented_map[k] = self.analyze_ordered_dict_object(v)
            return commented_map
        return data

    def __delitem__(self, key):
        try:
            super(Parser, self).__delitem__(key)
        except KeyError as e:
            logger.error(e)

    def update(self, other=None, **kwargs):
        if other is not None:
            for k, v in other.items() if isinstance(other, Mapping) else other:
                self[k] = v
        for k, v in kwargs.items():
            self[k] = v
        super(Parser, self).update(self)