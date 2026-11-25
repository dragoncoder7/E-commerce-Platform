"""Read an explicitly selected local captured test email; never calls a mail provider."""
import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser(description="读取本地捕获的商城验证码（不发送邮件）")
parser.add_argument("email")
args = parser.parse_args()
email = args.email.strip().lower()
path = Path(__file__).resolve().parents[1] / ".runtime" / "mail" / (hashlib.sha256(email.encode()).hexdigest() + ".json")
if not path.is_file():
    parser.error("未找到该邮箱的本地验证码，请先在注册页获取验证码")
print(json.dumps(json.loads(path.read_text(encoding="utf-8")), ensure_ascii=False, indent=2))
