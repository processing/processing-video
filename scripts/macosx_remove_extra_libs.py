#!/usr/bin/env python

# Adapted from:
# https://github.com/gohai/processing-glvideo/blob/master/src/native/macosx_remove_extra_libs.py

from __future__ import print_function
import os
import sys
import subprocess
import re

lib_folder = '../library/macosx64'

pattern = re.compile('@loader_path/([^ ]+) ')

required = []
to_check = []

# add all modules
for fn in os.listdir(lib_folder):
	if fn.endswith('.so') or fn.endswith('.dylib'):
		to_check.append(fn)

for fn in os.listdir(lib_folder + '/gstreamer-1.0'):
	if fn.endswith('.so') or fn.endswith('.dylib'):
		to_check.append('gstreamer-1.0/' + fn)

while 0 < len(to_check):
	tested = to_check.pop()
	print('otool -L ' + lib_folder + '/' + tested)
	out = subprocess.check_output('otool -L ' + lib_folder + '/' + tested, shell=True)
	if sys.version_info > (3, 0):
		out = out.decode()

	required.append(tested)
	deps = pattern.findall(out)
	for dep in deps:		
		# we're in the module directory, remove any trailing ../
		if '/' in tested and dep[0:3] == '../':
			dep = dep[3:]
		if dep not in required:
			required.append(dep)

required.sort()

# remove unneeded libs
for fn in os.listdir(lib_folder):
	if fn.endswith('.so') or fn.endswith('.dylib') and fn not in required:
		try:
			print('Removing ' + fn + ' ... ', end='')
			os.remove(lib_folder + '/' + fn)
			print('Done')
		except:
			print('Fail')
