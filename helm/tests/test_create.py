from pygluu.kubernetes.create import create_parser, main
import pygluu.kubernetes.create as module0
import argparse
import sys
import pytest


def test_empty_arg():
    parser = create_parser()
    args = parser.parse_args(['version'])

    assert args is not None


def test_main_exception():
    try:
        var0 = module0.main()
    except BaseException:
        pass


def test_create_exception():
    try:
        var0 = module0.create_parser()
    except BaseException:
        pass

