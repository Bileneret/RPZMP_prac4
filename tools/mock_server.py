#!/usr/bin/env python3
"""
Локальний mock-сервер для КП4 (Варіант 13: Система освітлення з IoT, ПЗ13–ПЗ16).
 Працює лише на стандартній бібліотеці Python (python 3.10+).

Приклади запуску:
  python tools/mock_server.py --port 8081
  python tools/mock_server.py --port 8081 --reset
  python tools/mock_server.py --port 8081 --fault 500
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import threading
import time
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

TOOLS_DIR = Path(__file__).resolve().parent
STATE_FILE = TOOLS_DIR / "server_state.json"
FAULT_FILE = TOOLS_DIR / "fault_mode.json"

DEFAULT_TTL_SEC = 86_400
MOCK_SALT = "kpi-iot-lighting-prac4-demo"
VALID_FAULTS = {"none", "slow", "500", "429", "401"}

# 25 перевірених реальних вузлів освітлення м. Кривий Ріг (звірено через Nominatim/OSM, охоплюють 6 районів):
# магістралі, ТРЦ, КНУ, стадіон «Металург», вокзал «Кривий Ріг-Головний», парки та віддалений Інгулець (#9).
SEED_LIGHTS: list[dict[str, Any]] = [
    {
        "id": 1,
        "name": "Магістраль пр. Поштовий (Карнаватка)",
        "zone": "Вулиця",
        "powerW": 150,
        "isPoweredOn": True,
        "latitude": 47.919217,
        "longitude": 33.356511,
    },
    {
        "id": 2,
        "name": "Магістраль вул. Соборності (Соцмісто)",
        "zone": "Вулиця",
        "powerW": 145,
        "isPoweredOn": True,
        "latitude": 47.900649,
        "longitude": 33.405048,
    },
    {
        "id": 3,
        "name": "Магістраль пр. Миру (95 квартал)",
        "zone": "Вулиця",
        "powerW": 150,
        "isPoweredOn": True,
        "latitude": 47.907563,
        "longitude": 33.386073,
    },
    {
        "id": 4,
        "name": "Вулиця Героїв АТО (96 квартал)",
        "zone": "Вулиця",
        "powerW": 140,
        "isPoweredOn": False,
        "latitude": 47.906481,
        "longitude": 33.408299,
    },
    {
        "id": 5,
        "name": "Алея вул. Віталія Матусевича",
        "zone": "Вулиця",
        "powerW": 130,
        "isPoweredOn": True,
        "latitude": 47.904497,
        "longitude": 33.394979,
    },
    {
        "id": 6,
        "name": "Магістраль пр. Металургів (Стріла)",
        "zone": "Вулиця",
        "powerW": 150,
        "isPoweredOn": True,
        "latitude": 47.897020,
        "longitude": 33.394239,
    },
    {
        "id": 7,
        "name": "Вулиця Кобилянського (Шахтопрохідка)",
        "zone": "Вулиця",
        "powerW": 135,
        "isPoweredOn": False,
        "latitude": 47.897400,
        "longitude": 33.362596,
    },
    {
        "id": 8,
        "name": "Магістраль вул. Січеславської (КРЕС)",
        "zone": "Вулиця",
        "powerW": 140,
        "isPoweredOn": True,
        "latitude": 47.988740,
        "longitude": 33.438355,
    },
    {
        "id": 9,
        "name": "Проспект Перемоги (Інгулець)",
        "zone": "Вулиця",
        "powerW": 150,
        "isPoweredOn": True,
        "latitude": 47.683482,
        "longitude": 33.163453,
    },
    {
        "id": 10,
        "name": "Вулиця Володимира Великого",
        "zone": "Вулиця",
        "powerW": 130,
        "isPoweredOn": True,
        "latitude": 47.945350,
        "longitude": 33.426139,
    },
    {
        "id": 11,
        "name": "Вулиця Генерала Радієвського",
        "zone": "Вулиця",
        "powerW": 135,
        "isPoweredOn": False,
        "latitude": 47.943293,
        "longitude": 33.435135,
    },
    {
        "id": 12,
        "name": "Вулиця Миколи Світальського (Дубова балка)",
        "zone": "Вулиця",
        "powerW": 140,
        "isPoweredOn": True,
        "latitude": 47.971242,
        "longitude": 33.447372,
    },
    {
        "id": 13,
        "name": "Паркінг ТРК «Терра» (5-й Зарічний, 11А)",
        "zone": "Паркування",
        "powerW": 110,
        "isPoweredOn": True,
        "latitude": 48.001150,
        "longitude": 33.482281,
    },
    {
        "id": 14,
        "name": "Паркінг ТРЦ «Сонячна Галерея» (майдан Химиченка)",
        "zone": "Паркування",
        "powerW": 110,
        "isPoweredOn": True,
        "latitude": 47.946885,
        "longitude": 33.440868,
    },
    {
        "id": 15,
        "name": "Паркінг ТРЦ «Victory Plaza» (Центральний пр.)",
        "zone": "Паркування",
        "powerW": 100,
        "isPoweredOn": False,
        "latitude": 47.904265,
        "longitude": 33.357719,
    },
    {
        "id": 16,
        "name": "Фасад головного корпусу КНУ (Братів Кожакіних)",
        "zone": "Фасад",
        "powerW": 100,
        "isPoweredOn": True,
        "latitude": 47.905689,
        "longitude": 33.401417,
    },
    {
        "id": 17,
        "name": "Алея парку ім. Федора Мершавцева (Гданцівка)",
        "zone": "Парк",
        "powerW": 80,
        "isPoweredOn": True,
        "latitude": 47.899752,
        "longitude": 33.331585,
    },
    {
        "id": 18,
        "name": "Алея парку Героїв (Соцмісто)",
        "zone": "Парк",
        "powerW": 80,
        "isPoweredOn": True,
        "latitude": 47.905447,
        "longitude": 33.390146,
    },
    {
        "id": 19,
        "name": "Алея парку «Шахтарський» (Ставки)",
        "zone": "Парк",
        "powerW": 85,
        "isPoweredOn": False,
        "latitude": 48.026650,
        "longitude": 33.477442,
    },
    {
        "id": 20,
        "name": "Фасад стадіону «Металург»",
        "zone": "Фасад",
        "powerW": 120,
        "isPoweredOn": True,
        "latitude": 47.895629,
        "longitude": 33.395118,
    },
    {
        "id": 21,
        "name": "Магістраль площі Захисників України",
        "zone": "Вулиця",
        "powerW": 145,
        "isPoweredOn": True,
        "latitude": 47.908416,
        "longitude": 33.343479,
    },
    {
        "id": 22,
        "name": "Магістраль пр. 200-річчя Кривого Рогу",
        "zone": "Вулиця",
        "powerW": 150,
        "isPoweredOn": True,
        "latitude": 47.950239,
        "longitude": 33.435728,
    },
    {
        "id": 23,
        "name": "Вулиця Тбіліська (71 квартал)",
        "zone": "Вулиця",
        "powerW": 130,
        "isPoweredOn": False,
        "latitude": 47.880468,
        "longitude": 33.364902,
    },
    {
        "id": 24,
        "name": "Вулиця Саксаганська (Бажанове)",
        "zone": "Вулиця",
        "powerW": 135,
        "isPoweredOn": True,
        "latitude": 47.992298,
        "longitude": 33.444048,
    },
    {
        "id": 25,
        "name": "Фасад вокзалу «Кривий Ріг-Головний»",
        "zone": "Фасад",
        "powerW": 100,
        "isPoweredOn": True,
        "latitude": 47.912563,
        "longitude": 33.451650,
    },
]


def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def create_mock_jwt(email: str, role: str) -> str:
    now = int(time.time())
    header = _b64url(b'{"alg":"HS256","typ":"JWT"}')
    payload_bytes = json.dumps(
        {"sub": email, "role": role, "iat": now, "exp": now + DEFAULT_TTL_SEC},
        separators=(",", ":"),
        ensure_ascii=False,
    ).encode("utf-8")
    payload = _b64url(payload_bytes)
    signing_input = f"{header}.{payload}.{MOCK_SALT}".encode("utf-8")
    sig = _b64url(hashlib.sha256(signing_input).digest())
    return f"{header}.{payload}.{sig}"


def decode_mock_jwt(token: str) -> dict[str, Any] | None:
    parts = token.split(".")
    if len(parts) != 3 or not all(parts):
        return None
    payload_b64 = parts[1]
    padding = "=" * (-len(payload_b64) % 4)
    try:
        decoded = base64.urlsafe_b64decode(payload_b64 + padding).decode("utf-8")
        data = json.loads(decoded)
    except Exception:
        return None
    exp = data.get("exp", 0)
    if not isinstance(exp, (int, float)) or time.time() >= exp:
        return None
    return data


class MockStore:
    def __init__(self, reset: bool = False, initial_fault: str = "none") -> None:
        self._lock = threading.Lock()
        if reset or not STATE_FILE.exists():
            now_ms = int(time.time() * 1000)
            self._lights = [
                {**item, "lastUpdatedAt": now_ms - (len(SEED_LIGHTS) - idx) * 60_000}
                for idx, item in enumerate(SEED_LIGHTS)
            ]
            self._next_id = max(item["id"] for item in self._lights) + 1
            self._persist_state()
        else:
            raw = json.loads(STATE_FILE.read_text(encoding="utf-8"))
            self._lights = raw.get("lights", [])
            self._next_id = raw.get("next_id", (max((x["id"] for x in self._lights), default=0) + 1))

        if initial_fault != "none" or not FAULT_FILE.exists():
            self._fault_mode = initial_fault if initial_fault in VALID_FAULTS else "none"
            self._persist_fault()
        else:
            fault_raw = json.loads(FAULT_FILE.read_text(encoding="utf-8"))
            mode = fault_raw.get("mode", "none")
            self._fault_mode = mode if mode in VALID_FAULTS else "none"

    def _persist_state(self) -> None:
        tmp = TOOLS_DIR / "server_state.json.tmp"
        tmp.write_text(
            json.dumps({"next_id": self._next_id, "lights": self._lights}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        os.replace(tmp, STATE_FILE)

    def _persist_fault(self) -> None:
        tmp = TOOLS_DIR / "fault_mode.json.tmp"
        tmp.write_text(json.dumps({"mode": self._fault_mode}, indent=2), encoding="utf-8")
        os.replace(tmp, FAULT_FILE)

    def get_fault(self) -> str:
        with self._lock:
            return self._fault_mode

    def set_fault(self, mode: str) -> str:
        if mode not in VALID_FAULTS:
            raise ValueError(f"Unsupported fault mode: {mode}")
        with self._lock:
            self._fault_mode = mode
            self._persist_fault()
            return self._fault_mode

    def list_lights(self) -> list[dict[str, Any]]:
        with self._lock:
            return [dict(item) for item in self._lights]

    def set_power(self, device_id: int, powered_on: bool) -> dict[str, Any] | None:
        with self._lock:
            for item in self._lights:
                if item["id"] == device_id:
                    item["isPoweredOn"] = bool(powered_on)
                    item["lastUpdatedAt"] = int(time.time() * 1000)
                    self._persist_state()
                    return dict(item)
            return None

    def create_light(
        self,
        name: str,
        zone: str,
        power_w: int,
        latitude: float,
        longitude: float,
    ) -> dict[str, Any]:
        with self._lock:
            now_ms = int(time.time() * 1000)
            new_item = {
                "id": self._next_id,
                "name": name,
                "zone": zone,
                "powerW": power_w,
                "isPoweredOn": True,
                "latitude": latitude,
                "longitude": longitude,
                "lastUpdatedAt": now_ms,
            }
            self._next_id += 1
            self._lights.append(new_item)
            self._persist_state()
            return dict(new_item)

    def delete_light(self, device_id: int) -> bool:
        with self._lock:
            before = len(self._lights)
            self._lights = [item for item in self._lights if item["id"] != device_id]
            if len(self._lights) != before:
                self._persist_state()
                return True
            return False


class LightingRequestHandler(BaseHTTPRequestHandler):
    store: MockStore  # injected on subclass
    server_version = "IoTLightingMock/4.0"

    def log_message(self, format: str, *args: Any) -> None:
        ts = time.strftime("%H:%M:%S")
        print(f"[{ts}] {self.address_string()} - {format % args}")

    def _send_json(self, status: int, payload: Any) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self) -> dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0:
            return {}
        raw = self.rfile.read(length).decode("utf-8")
        return json.loads(raw)

    def _maybe_inject_fault(self, path: str) -> bool:
        if path.startswith("/debug/fault") or path == "/health":
            return False
        mode = self.store.get_fault()
        if mode == "slow":
            time.sleep(1.8)
            return False
        if mode == "500":
            self._send_json(500, {"error": "Simulated Internal Server Error"})
            return True
        if mode == "429":
            self._send_json(429, {"error": "Simulated Too Many Requests"})
            return True
        if mode == "401":
            self._send_json(401, {"error": "Simulated Unauthorized"})
            return True
        return False

    def _require_auth(self) -> dict[str, Any] | None:
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            self._send_json(HTTPStatus.UNAUTHORIZED, {"error": "Missing bearer token"})
            return None
        token = auth[len("Bearer "):].strip()
        claims = decode_mock_jwt(token)
        if claims is None:
            self._send_json(HTTPStatus.UNAUTHORIZED, {"error": "Invalid or expired token"})
            return None
        return claims

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            self._send_json(HTTPStatus.OK, {"status": "ok", "fault": self.store.get_fault()})
            return
        if self._maybe_inject_fault(path):
            return
        if path == "/debug/fault":
            if self._require_auth() is None:
                return
            self._send_json(HTTPStatus.OK, {"mode": self.store.get_fault()})
            return
        if path == "/lights":
            if self._require_auth() is None:
                return
            self._send_json(HTTPStatus.OK, self.store.list_lights())
            return
        self._send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        if self._maybe_inject_fault(path):
            return

        if path == "/auth/login":
            try:
                body = self._read_json()
            except Exception:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": "Invalid JSON"})
                return
            email = str(body.get("email", "")).strip()
            password = str(body.get("password", ""))
            if not email or len(password) < 4 or password == "wrong":
                self._send_json(HTTPStatus.UNAUTHORIZED, {"error": "Invalid credentials"})
                return
            email_lower = email.lower()
            if email_lower in ("admin@light.io", "admin@kpi.ua"):
                if password not in ("admin123", "admin"):
                    self._send_json(HTTPStatus.UNAUTHORIZED, {"error": "Invalid admin password"})
                    return
                role = "admin"
            elif email_lower in ("user@light.io", "operator@kpi.ua"):
                if password not in ("user123", "1234", "user1234", "pass1234"):
                    self._send_json(HTTPStatus.UNAUTHORIZED, {"error": "Invalid user password"})
                    return
                role = "user"
            else:
                role = "admin" if "admin" in email_lower else "user"

            token = create_mock_jwt(email=email, role=role)
            self._send_json(HTTPStatus.OK, {"token": token, "email": email, "role": role})
            return

        claims = self._require_auth()
        if claims is None:
            return

        if path == "/debug/fault":
            try:
                body = self._read_json()
                mode = self.store.set_fault(str(body.get("mode", "none")))
                self._send_json(HTTPStatus.OK, {"mode": mode})
            except Exception as exc:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})
            return

        if path == "/lights":
            if claims.get("role") != "admin":
                self._send_json(HTTPStatus.FORBIDDEN, {"error": "Admin role required"})
                return
            try:
                body = self._read_json()
                name = str(body.get("name", "")).strip()
                zone = str(body.get("zone", "")).strip()
                power_w = int(body.get("powerW", 0))
                lat = float(body.get("latitude", 47.9635))
                lng = float(body.get("longitude", 33.4325))
                if not name or not zone or power_w <= 0:
                    raise ValueError("Invalid name, zone or powerW")
                created = self.store.create_light(name, zone, power_w, lat, lng)
                self._send_json(HTTPStatus.CREATED, created)
            except Exception as exc:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})

    def do_PUT(self) -> None:
        path = urlparse(self.path).path
        if self._maybe_inject_fault(path):
            return
        if self._require_auth() is None:
            return

        parts = [p for p in path.split("/") if p]
        if len(parts) == 3 and parts[0] == "lights" and parts[2] == "power":
            try:
                device_id = int(parts[1])
                body = self._read_json()
                powered_on = bool(body.get("isPoweredOn"))
            except Exception:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": "Invalid power payload"})
                return
            updated = self.store.set_power(device_id, powered_on)
            if updated is None:
                self._send_json(HTTPStatus.NOT_FOUND, {"error": "Light device not found"})
                return
            self._send_json(HTTPStatus.OK, updated)
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})

    def do_DELETE(self) -> None:
        path = urlparse(self.path).path
        if self._maybe_inject_fault(path):
            return
        claims = self._require_auth()
        if claims is None:
            return
        if claims.get("role") != "admin":
            self._send_json(HTTPStatus.FORBIDDEN, {"error": "Admin role required"})
            return

        parts = [p for p in path.split("/") if p]
        if len(parts) == 2 and parts[0] == "lights":
            try:
                device_id = int(parts[1])
            except ValueError:
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": "Invalid device id"})
                return
            if self.store.delete_light(device_id):
                self._send_json(HTTPStatus.OK, {"deletedId": device_id})
            else:
                self._send_json(HTTPStatus.NOT_FOUND, {"error": "Device not found"})
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"error": "Not found"})


class ReusableThreadingHTTPServer(ThreadingHTTPServer):
    allow_reuse_address = True


def main() -> None:
    parser = argparse.ArgumentParser(description="IoT Lighting Prac4 Mock API Server")
    parser.add_argument("--host", default="127.0.0.1", help="Bind address (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=8081, help="Bind port (default: 8081)")
    parser.add_argument("--reset", action="store_true", help="Reset seed data before start")
    parser.add_argument("--fault", default="none", choices=sorted(VALID_FAULTS), help="Initial fault mode")
    args = parser.parse_args()

    store = MockStore(reset=args.reset, initial_fault=args.fault)

    class Handler(LightingRequestHandler):
        pass

    Handler.store = store
    server = ReusableThreadingHTTPServer((args.host, args.port), Handler)
    print(f"IoT Lighting Mock Server (КП4) listening on http://{args.host}:{args.port} (fault={store.get_fault()})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down mock server...")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
