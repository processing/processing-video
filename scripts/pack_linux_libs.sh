#!/bin/bash

# Set the appropriae build dist folder in this env variable
meson_build_folder=/home/andres/code/gstreamer/build-1.20/lib/x86_64-linux-gnu

# Copy the build to the native library folder for linux
mkdir ../library/linux-amd64
cp -a ${meson_build_folder}/* ../library/linux-amd64

# Remove static .a libs
rm -r ../library/linux-amd64/*.a

# Remove unncessary folders
rm -r ../library/linux-amd64/cairo
rm -r ../library/linux-amd64/cmake
rm -r ../library/linux-amd64/gio
rm -r ../library/linux-amd64/glib-2.0
rm -r ../library/linux-amd64/gst-validate-launcher
rm -r ../library/linux-amd64/pkgconfig
rm -r ../library/linux-amd64/gstreamer-1.0/validate