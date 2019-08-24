#!/usr/bin/env python

# This script removes base libraries that are not present as the dependency of any other library 
# in the distribution

# Original version by Gottfried Haider:
# https://github.com/gohai/processing-glvideo/blob/master/src/native/macosx_remove_extra_libs.py

from __future__ import print_function
import os
import sys
import subprocess
import re

lib_folder = '../library/macosx'

# First, remove libraries from packages we don't bundle with the video library:
# gstreamer-1.0-codecs-gpl
# gstreamer-1.0-codecs-restricted
# gstreamer-1.0-net-restricted
# gstreamer-1.0-effects
# gstreamer-1.0-encoding
# gstreamer-1.0-visualizers
# gstreamer-1.0-devtools
# gstreamer-1.0-dvd

libs_to_remove = [
    "liba52.0.dylib",
    "liba52.dylib",
    "libass.9.dylib",
    "libass.dylib",
    "libdca.0.dylib",
    "libdca.dylib",
    "libdvdnav.4.dylib",
    "libdvdnav.dylib",
    "libdvdread.4.dylib",
    "libdvdread.dylib",
    "libmms.0.dylib",
    "libmms.dylib",
    "libopencore-amrnb.0.dylib",
    "libopencore-amrnb.dylib",
    "libopencore-amrwb.0.dylib",
    "libopencore-amrwb.dylib",
    "librtmp.1.dylib",
    "librtmp.dylib",
    "libSoundTouch.1.dylib",
    "libSoundTouch.dylib",
    "libvisual-0.4.0.dylib",
    "libvisual-0.4.dylib",
    "libvo-aacenc.0.dylib",
    "libvo-aacenc.dylib",
    "libwebrtc_audio_processing.0.dylib",
    "libwebrtc_audio_processing.dylib",
    "libx264.148.dylib",
    "libx264.dylib"]

plugins_to_remove = [
    "libgsta52dec.dylib",
    "libgstaccurip.dylib",
    "libgstaiff.dylib",
    "libgstalpha.dylib",
    "libgstalphacolor.dylib",
    "libgstamrnb.dylib",
    "libgstamrwbdec.dylib",
    "libgstasf.dylib",
    "libgstasfmux.dylib",
    "libgstassrender.dylib",
    "libgstaudiobuffersplit.dylib",
    "libgstaudiofx.dylib",
    "libgstaudiofxbad.dylib",
    "libgstaudiolatency.dylib",
    "libgstaudiovisualizers.dylib",
    "libgstautoconvert.dylib",
    "libgstbayer.dylib",
    "libgstcairo.dylib",
    "libgstclosedcaption.dylib",
    "libgstcoloreffects.dylib",
    "libgstcutter.dylib",
    "libgstdebug.dylib",
    "libgstdebugutilsbad.dylib",
    "libgstdeinterlace.dylib",
    "libgstdtmf.dylib",
    "libgstdtsdec.dylib",
    "libgstdvdlpcmdec.dylib",
    "libgstdvdread.dylib",
    "libgstdvdsub.dylib",
    "libgsteffectv.dylib",
    "libgstencoding.dylib",
    "libgstequalizer.dylib",
    "libgstfieldanalysis.dylib",
    "libgstfreeverb.dylib",
    "libgstfrei0r.dylib",
    "libgstgaudieffects.dylib",
    "libgstgdkpixbuf.dylib",
    "libgstgeometrictransform.dylib",
    "libgstgoom.dylib",
    "libgstgoom2k1.dylib",
    "libgstimagefreeze.dylib",
    "libgstinter.dylib",
    "libgstinterlace.dylib",
    "libgstinterleave.dylib",
    "libgstivtc.dylib",
    "libgstladspa.dylib",
    "libgstlegacyrawparse.dylib",
    "libgstlevel.dylib",
    "libgstlibvisual.dylib",
    "libgstmms.dylib",
    "libgstmpegpsdemux.dylib",
    "libgstmpegpsmux.dylib",
    "libgstmpegtsdemux.dylib",
    "libgstmpegtsmux.dylib",
    "libgstmultifile.dylib",
    "libgstproxy.dylib",
    "libgstrealmedia.dylib",
    "libgstremovesilence.dylib",
    "libgstreplaygain.dylib",
    "libgstresindvd.dylib",
    "libgstrtmp.dylib",
    "libgstsegmentclip.dylib",
    "libgstshapewipe.dylib",   
    "libgstsmooth.dylib",
    "libgstsmpte.dylib",
    "libgstsoundtouch.dylib",
    "libgstspectrum.dylib",
    "libgstspeed.dylib",
    "libgstvideobox.dylib",
    "libgstvideocrop.dylib",
    "libgstvideofiltersbad.dylib",
    "libgstvideomixer.dylib",
    "libgstvoaacenc.dylib",
    "libgstwebrtcdsp.dylib",
    "libgstx264.dylib",
    "libgstxingmux.dylib"]

for name in libs_to_remove:
	fn = lib_folder + '/' + name
	if os.path.exists(fn):
		try:
			print('Removing extra ' + fn + ' ... ', end='')
			os.remove(fn)
			print('Done')
		except:
			print('Fail')
	else: 
		print("Library", name, "does not exist")

for name in plugins_to_remove:
	fn = lib_folder + '/gstreamer-1.0/' + name
	if os.path.exists(fn):
		try:
			print('Removing extra ' + fn + ' ... ', end='')
			os.remove(fn)
			print('Done')
		except:
			print('Fail')
	else:
		print("Plugin", name, "does not exist")

# Removing duplicated files...

exclude = ["libavcodec.58.dylib",
           "libavfilter.7.dylib",
           "libavformat.58.dylib",
           "libavutil.56.dylib",           
           "libhogweed.4.dylib",
           "libnettle.6.dylib",
           "libopenh264.4.dylib",           
           "libopenjp2.7.dylib",
           "libsrtp.1.dylib",
           "libsrt.1.dylib",
           "libswresample.3.dylib",           
           "libtag.1.dylib",
           "libz.1.dylib"]

files = []

# add all modules
for fn in os.listdir(lib_folder):
	if fn.endswith('.so') or fn.endswith('.dylib'):
		files.append(fn)

for fn in files:
	p = fn.find('.')
	prefix = fn[0:p]
	for fn1 in files:
		if len(fn) <= len(fn1): continue
		if prefix in fn1 and os.path.exists(lib_folder + '/' + fn1) and not fn1 in exclude:
			try:
				print('Removing duplicate ' + fn1 + ' ... ', end='')
				os.remove(lib_folder + '/' + fn1)
				print('Done')
			except:
				print('Fail')

# Now, removing libraries that are not depended upon...

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
	required.append(tested)	
	out = subprocess.check_output('otool -L ' + lib_folder + '/' + tested, shell=True)
	if sys.version_info > (3, 0):
		out = out.decode()

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
			print('Removing unused ' + fn + ' ... ', end='')
			os.remove(lib_folder + '/' + fn)
			print('Done')
		except:
			print('Fail')
