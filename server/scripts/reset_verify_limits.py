"""Clear rate counters in the explicitly named disposable local verification Redis only.

Run before a repeated verification suite and before manual acceptance. Never clears sessions or codes.
"""
import json
import subprocess

container = "ecommerce-b2c-redis-1"
metadata = json.loads(subprocess.check_output(["docker", "inspect", container]))[0]
labels = metadata["Config"]["Labels"]
if labels.get("com.docker.compose.project") != "ecommerce-b2c" or labels.get("com.docker.compose.service") != "redis":
    raise SystemExit("Refusing to touch a non-verification Redis")
keys = subprocess.check_output(["docker", "exec", container, "redis-cli", "--scan", "--pattern", "store:limit:*"], text=True).splitlines()
for key in keys:
    if not key.startswith("store:limit:"):
        raise SystemExit("Unexpected Redis key")
    subprocess.run(["docker", "exec", container, "redis-cli", "DEL", key], check=True, capture_output=True)
print(f"Reset {len(keys)} local verification rate counters; sessions and email codes preserved")
