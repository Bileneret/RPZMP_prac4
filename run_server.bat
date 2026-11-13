@echo off
cd /d "%~dp0"
echo Starting IoT Lighting Prac4 Mock Server on port 8081...
echo Tip: run "adb reverse tcp:8081 tcp:8081" for USB/Wi-Fi debug device access.
python tools\mock_server.py --port 8081 %*
