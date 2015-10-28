#!/bin/bash

export ISUCON_BENCH_DATADIR=$(pwd)/json
cat ../data/source.json | jq '.[0]' | gradle run -Pargs="net.isucon.isucon5f.bench.Full 127.0.0.1 -p 9292"
