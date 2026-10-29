# Standard library + cryptography. Canonicalization is RFC 8785 for the subset used
# here: integers, booleans, null and BMP strings only (no floats, no astral code points).
import base64, hashlib, json, os, sys
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization

def b64u(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()

def jcs(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")

def load_key(path):
    with open(path, "r") as fh:
        seed = base64.urlsafe_b64decode(fh.read().strip() + "==")
    return Ed25519PrivateKey.from_private_bytes(seed)

cmd = sys.argv[1]
if cmd == "gen":
    key = Ed25519PrivateKey.generate()
    seed = key.private_bytes(serialization.Encoding.Raw, serialization.PrivateFormat.Raw,
                             serialization.NoEncryption())
    fd = os.open(sys.argv[2], os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as fh:
        fh.write(b64u(seed))
    pub = key.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
    print(b64u(pub))
elif cmd == "sign":
    key = load_key(sys.argv[2])
    doc = json.load(sys.stdin)
    doc.pop("signature", None)
    doc["signature"] = b64u(key.sign(jcs(doc)))
    sys.stdout.write(json.dumps(doc, separators=(",", ":"), ensure_ascii=False))
elif cmd == "tamper-signature":
    doc = json.load(sys.stdin)
    sig = doc["signature"]
    doc["signature"] = ("B" if sig[0] == "A" else "A") + sig[1:]
    sys.stdout.write(json.dumps(doc, separators=(",", ":"), ensure_ascii=False))
elif cmd == "digest":
    print("sha256:" + hashlib.sha256(jcs(json.load(sys.stdin))).hexdigest())
elif cmd == "sha256":
    print("sha256:" + hashlib.sha256(sys.argv[2].encode("utf-8")).hexdigest())
elif cmd == "one-day-ago":
    import datetime
    t = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=1)
    print(t.strftime("%Y-%m-%dT%H:%M:%SZ"))
elif cmd == "uuid":
    import uuid
    print(uuid.uuid4())
else:
    sys.exit("unknown command: " + cmd)
