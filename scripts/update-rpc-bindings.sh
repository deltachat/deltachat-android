#!/bin/sh
# you need to have dcrpcgen

# generate schema.json
ROOT_DIR=$PWD
cd ./jni/deltachat-core-rust/deltachat-rpc-server
cargo run -- --openrpc > "$ROOT_DIR/schema.json"
cd "$ROOT_DIR"

# generate code
dcrpcgen java --schema schema.json -o ./src/main/java/
