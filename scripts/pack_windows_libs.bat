SET gst_folder=C:\gstreamer
SET gst_toolchain=
SET lib_folder=..\library

echo "Copying base gstreamer libs..."
md %lib_folder%\windows64 %lib_folder%\windows32
copy %gst_folder%\1.0\%gst_toolchain%x86_64\bin\*.dll %lib_folder%\windows64
copy %gst_folder%\1.0\%gst_toolchain%x86\bin\*.dll %lib_folder%\windows32

echo "Copying gstreamer plugins..."
md %lib_folder%\windows64\gstreamer-1.0 %lib_folder%\windows32\gstreamer-1.0
copy %gst_folder%\1.0\%gst_toolchain%x86_64\lib\gstreamer-1.0\*.dll %lib_folder%\windows64\gstreamer-1.0
copy %gst_folder%\1.0\%gst_toolchain%x86\lib\gstreamer-1.0\*.dll %lib_folder%\windows32\gstreamer-1.0

echo "Remove broken plugins..."
del %lib_folder%\windows64\gstreamer-1.0\libgsta52dec.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstamrnb.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstamrwbdec.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstassrender.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstdtsdec.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstdvdread.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstges.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstlibvisual.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstmms.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstresindvd.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstrtmp.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstsoundtouch.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstvoaacenc.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstwebrtcdsp.dll /f
del %lib_folder%\windows64\gstreamer-1.0\libgstx264.dll /f

del %lib_folder%\windows32\gstreamer-1.0\libgsta52dec.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstamrnb.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstamrwbdec.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstassrender.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstdtsdec.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstdvdread.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstges.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstlibvisual.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstmms.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstresindvd.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstrtmp.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstsoundtouch.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstvoaacenc.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstwebrtcdsp.dll /f
del %lib_folder%\windows32\gstreamer-1.0\libgstx264.dll /f