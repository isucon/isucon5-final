#!/bin/sh
export PATH=/home/isucon/.local/ruby/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin
export LANG=ja_JP.UTF-8
export RACK_ENV=production

exec $*
