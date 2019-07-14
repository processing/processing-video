SET gst_folder=C:\gstreamer\1.0
SET lib_folder=..\library

echo "Copying base gstreamer libs..."
md %lib_folder%\windows64 %lib_folder%\windows32
copy %gst_folder%\x86_64\bin\*.dll %lib_folder%\windows64
copy %gst_folder%\x86\bin\*.dll %lib_folder%\windows32

echo "Copying gstreamer plugins..."
md %lib_folder%\windows64\gstreamer-1.0 %lib_folder%\windows32\gstreamer-1.0
copy %gst_folder%\x86_64\lib\gstreamer-1.0\*.dll %lib_folder%\windows64\gstreamer-1.0
copy %gst_folder%\x86\lib\gstreamer-1.0\*.dll %lib_folder%\windows32\gstreamer-1.0