#!/bin/bash

# Adapted from:
# https://github.com/gohai/processing-glvideo/blob/master/src/native/Makefile

gst_folder=/Library/Frameworks/GStreamer.framework/Libraries
lib_folder=../library/macosx64

echo "Copying base gstreamer libs..."
mkdir -p ${lib_folder}
cp ${gst_folder}/*.dylib ${lib_folder}

echo "Relocating dependencies in base libs..."
./macosx_relocator.py ${lib_folder} "@rpath/lib/" "@loader_path/"

echo "Copying gstreamer plugins..."
mkdir -p ${lib_folder}/gstreamer-1.0
cp /Library/Frameworks/GStreamer.framework/Libraries/gstreamer-1.0/* ${lib_folder}/gstreamer-1.0

echo "Relocating dependencies in gstreamer plugins..."
./macosx_relocator.py ${lib_folder}/gstreamer-1.0 "@rpath/lib/" "@loader_path/../"

# ./macosx_remove_extra_libs.py

# silence runtime error
rm -f ${lib_folder}/gstreamer-1.0/libgstopenjpeg.*