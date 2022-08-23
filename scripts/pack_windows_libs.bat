SET gst_folder=C:\gstreamer
SET gst_toolchain=msvc
SET lib_folder=..\library

echo "Copying base gstreamer libs..."
md %lib_folder%\windows-amd64
copy %gst_folder%\1.0\%gst_toolchain%_x86_64\bin\*.dll %lib_folder%\windows-amd64

echo "Copying gstreamer plugins..."
md %lib_folder%\windows-amd64\gstreamer-1.0
copy %gst_folder%\1.0\%gst_toolchain%_x86_64\lib\gstreamer-1.0\*.dll %lib_folder%\windows-amd64\gstreamer-1.0
