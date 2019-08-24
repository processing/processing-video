#!/bin/bash

# This script bundles the binaries from a GStreamer system installation on Mac
# into the Processing video library. It relocates the binaries so they can be
# used from Processing without requiring a GStreamer installation on the users'
# computers, as detailed in the GStreamer documentation:
# https://gstreamer.freedesktop.org/documentation/deploying/mac-osx.html?gi-language=c#relocation-of-gstreamer-in-os-x

# Original version by Gottfried Haider. 
# https://github.com/gohai/processing-glvideo/blob/master/src/native/Makefile

gst_minor_ver=${1:-16} 

if [ ${gst_minor_ver} -gt 14 ]
then
    dep_path="@rpath/lib/"
else
    dep_path="/Library/Frameworks/GStreamer.framework/Versions/1.0/lib/"
fi

gst_folder=/Library/Frameworks/GStreamer.framework/Versions/1.0/lib
lib_folder=../library/macosx

echo "Copying base gstreamer libs..."
mkdir -p ${lib_folder}
cp ${gst_folder}/*.dylib ${lib_folder}

echo "Relocating dependencies in base libs..."
./macosx_relocator.py ${lib_folder} ${dep_path} "@loader_path/"

echo "Copying gstreamer plugins..."
mkdir -p ${lib_folder}/gstreamer-1.0
cp ${gst_folder}/gstreamer-1.0/* ${lib_folder}/gstreamer-1.0

# Silence runtime error from these plugins
rm -f ${lib_folder}/gstreamer-1.0/libgsthls.so
rm -f ${lib_folder}/gstreamer-1.0/libgstopenjpeg.so

echo "Relocating dependencies in gstreamer plugins..."
./macosx_relocator.py ${lib_folder}/gstreamer-1.0 ${dep_path} "@loader_path/../"

echo "Removing unused dependencies..."
./macosx_remove_extra_libs.py