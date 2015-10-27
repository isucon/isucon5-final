# -*- coding: utf-8 -*-

"""
$ python3 hash.py --date="Tue, 27 Oct 2015 08:46:40 JST"
51498849
"""

import argparse
import hashlib
import struct

def toint(date, secret):
	s = date + " " + secret
	m = hashlib.md5(s.encode("utf-8"))
	d = m.digest()
	n = struct.unpack(">I", d[0:4])[0]
	return n

parser = argparse.ArgumentParser()
parser.add_argument('--date', type=str)
parser.add_argument('--secret', type=str, default="happyhalloween")
args = parser.parse_args()

n = toint(args.date, args.secret)
print("%s ==> %d" % (args.date, n))
