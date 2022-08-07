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
lib_folder_univ=../library/macos-universal
lib_folder_x86_64=../library/macos-x86_64
lib_folder_aarch64=../library/macos-aarch64

echo "Copying base gstreamer libs..."
mkdir -p ${lib_folder_univ}
cp ${gst_folder}/*.dylib ${lib_folder_univ}

echo "Relocating dependencies in base libs..."
./macosx_relocator.py ${lib_folder_univ} ${dep_path} "@loader_path/"

echo "Copying gstreamer plugins..."
mkdir -p ${lib_folder_univ}/gstreamer-1.0
cp ${gst_folder}/gstreamer-1.0/* ${lib_folder_univ}/gstreamer-1.0

# Remove plugins that give runtime errors:
rm -f ${lib_folder_univ}/gstreamer-1.0/libgstsrt.dylib
rm -f ${lib_folder_univ}/gstreamer-1.0/libgstsrtp.dylib

# These seem okay now (with GStreamer 1.20.x)
# rm -f ${lib_folder_univ}/gstreamer-1.0/libgsthls.so
# rm -f ${lib_folder_univ}/gstreamer-1.0/libgstopenjpeg.so

echo "Relocating dependencies in gstreamer plugins..."
./macosx_relocator.py ${lib_folder_univ}/gstreamer-1.0 ${dep_path} "@loader_path/../"

echo "Removing unused dependencies..."
./macosx_remove_extra_libs.py

echo "Extracting x86_64 and aarch64 native libraries..."

mkdir -p ${lib_folder_x86_64}
mkdir -p ${lib_folder_aarch64}
for file in ${lib_folder_univ}/*.dylib; do 
  fn="$(basename ${file})"
  lipo ${file} -thin x86_64 -output ${lib_folder_x86_64}/${fn};
  lipo ${file} -thin arm64 -output ${lib_folder_aarch64}/${fn};
done

mkdir -p ${lib_folder_x86_64}/gstreamer-1.0
mkdir -p ${lib_folder_aarch64}/gstreamer-1.0
for file in ${lib_folder_univ}/gstreamer-1.0/*.dylib; do 
  fn="$(basename ${file})"
  lipo ${file} -thin x86_64 -output ${lib_folder_x86_64}/gstreamer-1.0/${fn};
  lipo ${file} -thin arm64 -output ${lib_folder_aarch64}/gstreamer-1.0/${fn};
done

echo "Removing universal native libraries..."
rm -rf ${lib_folder_univ}

echo "Done."