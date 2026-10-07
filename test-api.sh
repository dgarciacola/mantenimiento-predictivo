#!/usr/bin/env bash
# test-api.sh - Prueba end-to-end de la API de sensores (backend en localhost:8080)
# Uso: ./test-api.sh [BASE_URL]
BASE="${1:-http://localhost:8080}/api/sensors"
SID="test-motor-$(date +%s)"
PASS=0; FAIL=0

# check "descripción" STATUS_ESPERADO "texto que debe aparecer en el body" METHOD URL [BODY]
check() {
  local desc="$1" expected="$2" contains="$3" method="$4" url="$5" body="$6"
  local out status resp
  if [ -n "$body" ]; then
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$url" -H "Content-Type: application/json" -d "$body")
  else
    out=$(curl -s -w $'\n%{http_code}' -X "$method" "$url")
  fi
  status=$(echo "$out" | tail -n1); resp=$(echo "$out" | sed '$d')
  if [ "$status" = "$expected" ] && echo "$resp" | grep -q "$contains"; then
    echo "✅ $desc (HTTP $status)"; PASS=$((PASS+1))
  else
    echo "❌ $desc -> esperado $expected / '$contains', recibido $status"; echo "   $resp"; FAIL=$((FAIL+1))
  fi
}

echo "== Probando $BASE (sensor: $SID) =="
check "Health check"                         200 '"UP"'          GET  "$BASE/health"
check "POST lectura NORMAL"                  201 '"success"'     POST "$BASE/data" "{\"sensor_id\":\"$SID\",\"temperature\":45.3,\"vibration\":2.1,\"current\":15.2}"
check "POST lectura CRITICAL"                201 'CRITICAL'      POST "$BASE/data" "{\"sensor_id\":\"$SID\",\"temperature\":90,\"vibration\":8,\"current\":42}"
check "POST temperatura fuera de rango"      400 '"status":400'  POST "$BASE/data" "{\"sensor_id\":\"$SID\",\"temperature\":150,\"vibration\":2,\"current\":10}"
check "POST campo obligatorio ausente"       400 '"Validation Failed"' POST "$BASE/data" "{\"sensor_id\":\"$SID\",\"vibration\":2,\"current\":10}"
check "GET historial existente"              200 '"totalReadings"' GET "$BASE/$SID/history"
check "GET historial sensor inexistente"     404 '"Not Found"'   GET  "$BASE/motor-999-no-existe/history"
check "GET rango temporal válido"            200 '"readings"'    GET  "$BASE/$SID/history/time-range?startTime=2020-01-01T00:00:00&endTime=2099-01-01T00:00:00"
check "GET rango temporal invertido"         400 '"Invalid Sensor Data"' GET "$BASE/$SID/history/time-range?startTime=2099-01-01T00:00:00&endTime=2020-01-01T00:00:00"
check "GET lista de sensores"                200 "$SID"          GET  "$BASE/list"
check "GET alertas críticas"                 200 '"critical-alerts"' GET "$BASE/critical"

echo "== Resultado: $PASS OK, $FAIL fallos =="
[ "$FAIL" -eq 0 ]
