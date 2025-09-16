#!/usr/bin/env bash
set -euo pipefail

# ====== CONFIG ======
BROKER_URL="${BROKER_URL:-tcp://localhost:61616}"
USER="${ARTEMIS_USER:-admin}"
PASS="${ARTEMIS_PASSWORD:-admin}"

# If artemis is not on PATH, set the full path, e.g. /opt/apache-artemis/bin/artemis
ARTEMIS="${ARTEMIS:-artemis}"

echo "Using broker: $BROKER_URL"

create_address() {
  local addr="$1"
  echo "Creating address: $addr"
  "$ARTEMIS" address create \
    --name "$addr" \
    --routing-type ANYCAST \
    --user "$USER" --password "$PASS" --url "$BROKER_URL" \
    || echo "Address '$addr' may already exist. Continuing."
}

create_queue() {
  local addr="$1"
  local qname="$2"
  echo "Creating queue: $qname on $addr"
  "$ARTEMIS" queue create \
    --address "$addr" \
    --name "$qname" \
    --durable \
    --routing-type ANYCAST \
    --user "$USER" --password "$PASS" --url "$BROKER_URL" \
    || echo "Queue '$qname' may already exist. Continuing."
}

# ====== Addresses & Queues (demo) ======
# Core flows
# create_address "demo.payments.pps.bs.payment.request.v1.json"
create_queue   "demo.payments.pps.bs.payment.request.v1.json" "q.demo.payments.pps.bs.payment.request.v1"

# create_address "demo.payments.bs.pps.fraud.response.v1.json"
create_queue   "demo.payments.bs.pps.fraud.response.v1.json" "q.demo.payments.bs.pps.fraud.response.v1"

create_address "demo.payments.bs.fcs.fraud.request.v1.xml"
create_queue   "demo.payments.bs.fcs.fraud.request.v1.xml"   "q.demo.payments.bs.fcs.fraud.request.v1"

create_address "demo.payments.fcs.bs.fraud.response.v1.xml"
create_queue   "demo.payments.fcs.bs.fraud.response.v1.xml"  "q.demo.payments.fcs.bs.fraud.response.v1"

# DLQ / EXP (pick ONE approach)

# (A) Use built-in global DLQ/Expiry (already present in most brokers)
#     -> nothing to create here

# (B) Per-domain DLQ/EXP (recommended if you want isolation)
# Uncomment to create domain DLQ/EXP and then point address-settings to them.
# create_address "demo.payments.DLQ"
# create_queue   "demo.payments.DLQ" "demo.payments.DLQ"
# create_address "demo.payments.EXP"
# create_queue   "demo.payments.EXP" "demo.payments.EXP"

echo "Done. Remember to configure Address Settings (DLQ/Expiry/Redelivery) via console or broker.xml."
