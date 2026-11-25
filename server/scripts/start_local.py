"""Start the local verification apps from copied JARs so Maven can rebuild on Windows.

Infrastructure: docker compose --env-file .runtime/verify.env -f compose.b2c.yml -p ecommerce-b2c up -d mysql redis nacos
Run: python scripts/start_local.py [--restart]
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / ".runtime"
APPS = [("erp", "modules/erp/erp-service", 18081), ("store", "modules/store/store-service", 18082), ("gateway", "gateway", 18080)]


def listening(port):
    with socket.socket() as connection:
        connection.settimeout(1)
        return connection.connect_ex(("127.0.0.1", port)) == 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--restart", action="store_true")
    args = parser.parse_args()
    env = os.environ.copy()
    for line in (RUNTIME / "verify.env").read_text().splitlines():
        key, value = line.split("=", 1)
        env[key] = value
    env.update(STORE_MAIL_MODE="capture", STORE_MAIL_CAPTURE_PATH=str(RUNTIME / "mail"), SPRING_PROFILES_ACTIVE="verify", SPRING_DATASOURCE_URL="jdbc:mysql://localhost:13306/ecommerce_system?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
               SPRING_DATASOURCE_USERNAME="root", SPRING_DATASOURCE_PASSWORD=env["DB_PASSWORD"], SPRING_DATA_REDIS_HOST="localhost", SPRING_DATA_REDIS_PORT="16379",
               SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR="localhost:18848", SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR="localhost:18848",
               ERP_SERVICE_URI="http://localhost:18081", STORE_SERVICE_URI="http://localhost:18082", MEDIA_PATH=str(RUNTIME / "media"))
    for name, module, port in APPS:
        if args.restart and listening(port):
            # Kill only the exact Java verification process at the intended port.
            command = "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress"
            result = subprocess.run(["powershell", "-NoProfile", "-Command", command], capture_output=True, text=True, check=True)
            processes = json.loads(result.stdout or "[]")
            if isinstance(processes, dict):
                processes = [processes]
            matches = [entry for entry in processes if f"--server.port={port}" in (entry.get("CommandLine") or "")
                       and (f"{module}/target/" in entry["CommandLine"] or str(RUNTIME) in entry["CommandLine"])]
            if len(matches) != 1:
                raise RuntimeError(f"Cannot identify owned {name} process; refusing to stop another application")
            subprocess.run(["taskkill", "/PID", str(matches[0]["ProcessId"]), "/T", "/F"], check=True, capture_output=True)
            for _ in range(30):
                if not listening(port):
                    break
                time.sleep(0.2)
        if listening(port):
            print(f"{name}: existing listener at {port}; use --restart to load rebuilt code")
            continue
        source = ROOT / module / "target" / f"{name if name == 'gateway' else name + '-service'}-1.0.0.jar"
        destination = RUNTIME / f"{name}-{time.time_ns()}.jar"
        shutil.copy2(source, destination)
        with (RUNTIME / f"{name}.log").open("w", encoding="utf-8") as log:
            process = subprocess.Popen(["java", "-Xmx384m", "-Duser.timezone=Asia/Shanghai", "-jar", str(destination), f"--server.port={port}"],
                                       cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        (RUNTIME / f"{name}.pid").write_text(str(process.pid))
        print(f"{name}: started PID {process.pid} at {port}")
    for name, _, port in APPS:
        endpoint = "/api/store/catalog/categories" if name == "gateway" else "/v3/api-docs"
        for attempt in range(60):
            try:
                with urllib.request.urlopen(f"http://127.0.0.1:{port}{endpoint}", timeout=2) as response:
                    if response.status == 200:
                        print(f"{name}: HTTP ready")
                        break
            except (OSError, TimeoutError):
                time.sleep(0.5)
        else:
            raise RuntimeError(f"{name} not ready; inspect .runtime/{name}.log")


if __name__ == "__main__":
    main()
