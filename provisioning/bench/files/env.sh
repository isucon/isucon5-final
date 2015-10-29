#!/bin/sh
export PATH=/home/isucon/.local/ruby/bin:/usr/local/bin:/usr/local/bin:/usr/bin:/bin
export LANG=ja_JP.UTF-8
export RACK_ENV=production
export _JAVA_OPTIONS="-Dfile.encoding=UTF8 -Duser.timezone=JST"
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

exec $*
