#!/usr/bin/env bash
set -euo pipefail

# ====== CONFIG ======
CONTAINER="${ARTEMIS_CONTAINER:-activemq-artemis}"   
BROKER_URL="${BROKER_URL:-tcp://localhost:61616}"
USER="${ARTEMIS_USER:-artemis}"
PASS="${ARTEMIS_PASSWORD:-artemis}"

ARTEMIS_CMD_PATH="/var/lib/artemis-instance/bin/artemis"

# PODMAN
ARTEMIS_CMD=(podman exec -i "$CONTAINER" "$ARTEMIS_CMD_PATH")
# DOCKER
# ARTEMIS_CMD=(docker exec -i "$CONTAINER" "$ARTEMIS_CMD_PATH")

echo "Using container: $CONTAINER ($BROKER_URL)"

create_address() {
  local addr="$1"
  echo "Deleting address: $addr"
  "${ARTEMIS_CMD[@]}" address create \
    --name "$addr" \
    --anycast --no-multicast --silent \
    --user "$USER" --password "$PASS" --url "$BROKER_URL"
}

create_queue() {
  local addr="$1"; local qname="$2"
  echo "Creating queue: $qname on $addr"
  "${ARTEMIS_CMD[@]}" queue create \
    --address "$addr" \
    --name "$qname" \
    --durable --anycast --preserve-on-no-consumers --auto-create-address --silent \
    --user "$USER" --password "$PASS" --url "$BROKER_URL"
}

delete_queue() {
  local local qname="$1"
  echo "Deleting queue: $qname"
  "${ARTEMIS_CMD[@]}" queue delete \
    --name "$qname" --removeConsumers --autoDeleteAddress --silent \
    --user "$USER" --password "$PASS" --url "$BROKER_URL"
}

delete_address() {
  local addr="$1"
  echo "Force Deleting address: $addr"
  "${ARTEMIS_CMD[@]}" address delete \
    --name "$addr" \
    --force \
    --user "$USER" --password "$PASS" --url "$BROKER_URL"
}

# ====== Addresses & Queues (demo) ======
delete_address "demo.payments.pps.bs.payment.request.v1.json"
# delete_queue  "q.demo.payments.pps.bs.payment.request.v1"

delete_address "demo.payments.bs.pps.fraud.response.v1.json"
# delete_queue   "q.demo.payments.bs.pps.fraud.response.v1"

delete_address "demo.payments.bs.fcs.fraud.request.v1.xml"
# delete_queue   "q.demo.payments.bs.fcs.fraud.request.v1"

delete_address "demo.payments.fcs.bs.fraud.response.v1.xml"
# delete_queue   "q.demo.payments.fcs.bs.fraud.response.v1"

# (Optional) per-domain DLQ/EXP
# create_address "demo.payments.DLQ"
# create_queue   "demo.payments.DLQ" "demo.payments.DLQ"
# create_address "demo.payments.EXP"
# create_queue   "demo.payments.EXP" "demo.payments.EXP"

echo "Done. Configure Address Settings via console or broker.xml."
