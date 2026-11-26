"""Make the loader's ``scripts`` folder importable, as the image does by running the scripts from that folder."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))
