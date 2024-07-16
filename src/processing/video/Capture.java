/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-22 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  GStreamer implementation ported from GSVideo library by Andres Colubri
  The previous version of this code was developed by Hernando Barragan


  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;

import processing.core.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.EnumSet;
import java.util.List;
import java.lang.reflect.*;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.device.*;
import org.freedesktop.gstreamer.elements.*;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.event.SeekType;


/**
 * Datatype for storing and manipulating video frames from an attached 
 * capture device such as a camera. Use <b>Capture.list()</b> to show 
 * the names of any attached devices. Using the version of the constructor 
 * without <b>name</b> will attempt to use the last device used by a 
 * QuickTime program.
 *
 * @webref capture
 * @webBrief Datatype for storing and manipulating video frames from an 
 * attached capture device such as a camera.
 * @usage application
 */
public class Capture extends PImage implements PConstants {
  public Pipeline pipeline;

  // The source resolution and framerate of the device
  public int sourceWidth;
  public int sourceHeight;
  public float sourceFrameRate;

  public float frameRate;
  protected float rate;

  protected boolean capturing = false;

  protected Method captureEventMethod;
  protected Object eventHandler;

  protected boolean available;
  protected boolean ready;
  protected boolean newFrame;

  protected AppSink rgbSink = null;
  protected int[] copyPixels = null;

  protected boolean firstFrame = true;

  protected boolean useBufferSink = false;
  protected boolean outdatedPixels = true;
  protected Object bufferSink;
  protected Method sinkCopyMethod;
  protected Method sinkSetMethod;
  protected Method sinkDisposeMethod;
  protected Method sinkGetMethod;

  protected String device;
  protected static List<Device> devices;    // we're caching this list for speed reasons

  NewSampleListener newSampleListener;
  NewPrerollListener newPrerollListener;
  private final Lock bufferLock = new ReentrantLock();


  /**
   *  Open the default capture device
   *  @param parent PApplet, typically "this"
   */
  public Capture(PApplet parent) {
    // Attempt to use a default resolution
    this(parent, 640, 480, null, 30);
  }


  /**
   *  Open a specific capture device
   *  @param device device name
   *  @see Capture#list()
   *  @see Capture#listRawNames()
   */
  public Capture(PApplet parent, String device) {
    // Attempt to use a default resolution
    this(parent, 640, 480, device, 30);
  }


  /**
   *  Open the default capture device with a given resolution
   *  @param width width in pixels
   *  @param height height in pixels
   */
  public Capture(PApplet parent, int width, int height) {
    this(parent, width, height, null, 30);
  }


  /**
   *  Open the default capture device with a given resolution and framerate
   *  @param fps frames per second
   */
  public Capture(PApplet parent, int width, int height, float fps) {
    this(parent, width, height, null, fps);
  }


  /**
   *  Open a specific capture device with a given resolution
   *  @see Capture#list()
   */
  public Capture(PApplet parent, int width, int height, String device) {
    this(parent, width, height, device, 30);
  }


  /**
   *  Open a specific capture device with a given framerate
   *  @see Capture#list()
   */
  public Capture(PApplet parent, String device, float fps) {
    this(parent, 640, 480, device, fps);
  }


  /**
   *  Open a specific capture device with a given resolution and framerate
   *  @see Capture#list()
   */
  public Capture(PApplet parent, int width, int height, String device, float fps) {
    super(width, height, RGB);
    this.device = device;
    this.frameRate = fps;
    initGStreamer(parent);
  }


  /**
   * Disposes all the native resources associated to this capture device.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public void dispose() {
    if (pipeline != null) {
      try {
        if (pipeline.isPlaying()) {
          pipeline.stop();
          pipeline.getState();
        }
      } catch (Exception e) {
      }

      pixels = null;

      if (rgbSink != null) {
        rgbSink.disconnect(newSampleListener);
        rgbSink.disconnect(newPrerollListener);
        rgbSink.dispose();
      }
      pipeline.setState(org.freedesktop.gstreamer.State.NULL);
      pipeline.getState();
      pipeline.getBus().dispose();
      pipeline.dispose();

      parent.g.removeCache(this);
      parent.unregisterMethod("dispose", this);
      parent.unregisterMethod("post", this);
    }
  }


  /**
   * Finalizer of the class.
   */
  protected void finalize() throws Throwable {
    try {
      dispose();
    } finally {
//      super.finalize();
    }
  }


  /**
   * Sets how often frames are read from the capture device. Setting the <b>fps</b>
   * parameter to 4, for example, will cause 4 frames to be read per second.
   *
   * @webref capture
   * @webBrief Sets how often frames are read from the capture device.
   * @usage web_application
   * @param ifps speed of the capture device in frames per second
   * @brief Sets the target frame rate
   */
  public void frameRate(float ifps) {
    float f = (0 < ifps && 0 < frameRate) ? ifps / frameRate : 1;

    long t = pipeline.queryPosition(TimeUnit.NANOSECONDS);
    long start, stop;
    if (rate > 0) {
      start = t;
      stop = -1;
    } else {
      start = 0;
      stop = t;
    }

    seek(rate * f, start, stop);

    frameRate = ifps;
  }


  /**
   * Returns "true" when a new frame from the device is available to read.
   *
   * @webref capture
   * @webBrief Returns "true" when a new frame from the device is available to read.
   * @usage web_application
   * @brief Returns "true" when a new frame is available to read.
   */
  public boolean available() {
    return available;
  }


  /**
   * Starts capturing frames from the selected device.
   *
   * @webref capture
   * @webBrief Starts capturing frames from an attached device.
   * @usage web_application
   * @brief Starts video capture
   */
  public void start() {
    setReady();

    pipeline.play();
    pipeline.getState();

    capturing = true;
  }


  /**
   * Stops capturing frames from an attached device.
   *
   * @webref capture
   * @webBrief Stops capturing frames from an attached device.
   * @usage web_application
   * @brief Stops video capture
   */
  public void stop() {
    setReady();

    pipeline.stop();
    pipeline.getState();

    capturing = false;
  }


  /**
   * Reads the current frame of the device.
   *
   * @webref capture
   * @webBrief Reads the current frame of the device.
   * @usage web_application
   * @brief Reads the current frame
   */
  public synchronized void read() {
    if (firstFrame) {
      super.init(sourceWidth, sourceHeight, RGB, 1);
      firstFrame = false;
    }

    if (useBufferSink) {

      if (bufferSink == null) {
        Object cache = parent.g.getCache(Capture.this);
        if (cache != null) {
          setBufferSink(cache);
          getSinkMethods();
        }
      }

    } else {
      int[] temp = pixels;
      pixels = copyPixels;
      updatePixels();
      copyPixels = temp;
    }

    available = false;
    newFrame = true;
  }


  /**
   * Loads the pixel data for the image into its <b>pixels[]</b> array.
   */
  @Override
  public synchronized void loadPixels() {
    super.loadPixels();

    if (useBufferSink && bufferSink != null) {
      try {
        // sinkGetMethod will copy the latest buffer to the pixels array,
        // and the pixels will be copied to the texture when the OpenGL
        // renderer needs to draw it.
        sinkGetMethod.invoke(bufferSink, new Object[] { pixels });
      } catch (Exception e) {
        e.printStackTrace();
      }

      outdatedPixels = false;
    }
  }


  /**
   * Reads the color of any pixel or grabs a section of an image.
   */
  @Override
  public int get(int x, int y) {
    if (outdatedPixels) loadPixels();
    return super.get(x, y);
  }


  /**
   * @param w width of pixel rectangle to get
   * @param h height of pixel rectangle to get
   */
  public PImage get(int x, int y, int w, int h) {
    if (outdatedPixels) loadPixels();
    return super.get(x, y, w, h);
  }


  @Override
  public PImage copy() {
    if (outdatedPixels) loadPixels();
    return super.copy();
  }


  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    if (outdatedPixels) loadPixels();
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
                  target, targetX, targetY);
  }


  /**
   * Check if this device object is currently capturing.
   */
  public boolean isCapturing() {
    return capturing;
  }


  ////////////////////////////////////////////////////////////

  // Initialization methods.


  protected void initGStreamer(PApplet parent) {
    this.parent = parent;
    pipeline = null;

    Video.init();

    if(device == null) {
      String[] devices = list();
      if(devices != null && devices.length > 0) {
        device = devices[0];
      } else {
        throw new IllegalStateException("Could not find any devices");
      }
    }

    device = device.trim();

    int p = device.indexOf("pipeline:");
    if (p == 0) {
      initCustomPipeline(device.substring(9));
    } else {
      initDevicePipeline();
    }

    try {
      // Register methods
      parent.registerMethod("dispose", this);
      parent.registerMethod("post", this);

      setEventHandlerObject(parent);

      sourceWidth = sourceHeight = 0;
      sourceFrameRate = -1;
      frameRate = -1;
      rate = 1.0f;
      ready = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public static String fpsToFramerate(float fps) {
    String formatted = Float.toString(fps);
    // This presumes the delimitter is always a dot
    int i = formatted.indexOf('.');
    if (Math.floor(fps) != fps) {
      int denom = (int)Math.pow(10, formatted.length()-i-1);
      int num = (int)(fps * denom);
      return num + "/" + denom;
    } else {
      return (int)fps + "/1";
    }
  }


  protected void initCustomPipeline(String pstr) {
    String PIPELINE_END = " ! videorate ! videoscale ! videoconvert ! appsink name=sink";

    pipeline = (Pipeline) Gst.parseLaunch(pstr + PIPELINE_END);

    String caps = ", width=" + width + ", height=" + height;
    if (frameRate != 0.0) {
      caps += ", framerate=" + fpsToFramerate(frameRate);
    }

    rgbSink = (AppSink) pipeline.getElementByName("sink");
    rgbSink.set("emit-signals", true);
    newSampleListener = new NewSampleListener();
    newPrerollListener = new NewPrerollListener();        
    rgbSink.connect(newSampleListener);
    rgbSink.connect(newPrerollListener);

    useBufferSink = Video.useGLBufferSink && parent.g.isGL();
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      if (useBufferSink) {
        rgbSink.setCaps(Caps.fromString("video/x-raw, format=RGBx" + caps));
      } else {
        rgbSink.setCaps(Caps.fromString("video/x-raw, format=BGRx" + caps));
      }
    } else {
      rgbSink.setCaps(Caps.fromString("video/x-raw, format=xRGB" + caps));
    }

    makeBusConnections(pipeline.getBus());
  }

  
  protected void initDevicePipeline() {
    Element srcElement = null;
    if (device == null) {
      // Use the default device from GStreamer
      srcElement = ElementFactory.make("autovideosrc", null);
    } else {
      // Look for device
      if (devices == null) {
        DeviceMonitor monitor = new DeviceMonitor();
        monitor.addFilter("Video/Source", null);
        devices = monitor.getDevices();
        monitor.close();
      }

      for (int i=0; i < devices.size(); i++) {
        String deviceName = assignDisplayName(devices.get(i), i);
        if (devices.get(i).getDisplayName().equals(device) || devices.get(i).getName().equals(device) || deviceName.equals(device)) {
          srcElement = devices.get(i).createElement(null);
          break;
        }
      }

      // Error out if we got passed an invalid device name
      if (srcElement == null) {
        throw new RuntimeException("Could not find device " + device);
      }
    }

    pipeline = new Pipeline();

    Element videoscale = ElementFactory.make("videoscale", null);
    Element videoconvert = ElementFactory.make("videoconvert", null);
    Element capsfilter = ElementFactory.make("capsfilter", null);

    String frameRateString;
    if (frameRate != 0.0) {
      frameRateString = ", framerate=" + fpsToFramerate(frameRate);
    } else {
      System.err.println("The capture framerate cannot be zero!");
      return;
    }

    capsfilter.set("caps", Caps.fromString("video/x-raw, width=" + width + ", height=" + height + frameRateString));

    initSink();

    pipeline.add(srcElement);
    pipeline.add(videoscale);
    pipeline.add(videoconvert);
    pipeline.add(capsfilter);
    pipeline.add(rgbSink);

    srcElement.link(videoscale);
    videoscale.link(videoconvert);
    videoconvert.link(capsfilter);
    capsfilter.link(rgbSink);

    makeBusConnections(pipeline.getBus());
  }


  /**
   * Uses a generic object as handler of the capture. This object should have a
   * captureEvent method that receives a Capture argument. This method will
   * be called upon a new frame read event.
   *
   */
  protected void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      captureEventMethod = eventHandler.getClass().getMethod("captureEvent", Capture.class);
      return;
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }

    // captureEvent can alternatively be defined as receiving an Object, to allow
    // Processing mode implementors to support the video library without linking
    // to it at build-time.
    try {
      captureEventMethod = eventHandler.getClass().getMethod("captureEvent", Object.class);
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }
  }


  protected void initSink() {
    rgbSink = new AppSink("capture sink");
    rgbSink.set("emit-signals", true);
    newSampleListener = new NewSampleListener();
    newPrerollListener = new NewPrerollListener();
    rgbSink.connect(newSampleListener);
    rgbSink.connect(newPrerollListener);

    useBufferSink = Video.useGLBufferSink && parent.g.isGL();
    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
      if (useBufferSink) rgbSink.setCaps(Caps.fromString("video/x-raw, format=RGBx"));
      else rgbSink.setCaps(Caps.fromString("video/x-raw, format=BGRx"));
    } else {
      rgbSink.setCaps(Caps.fromString("video/x-raw, format=xRGB"));
    }
  }


  protected void setReady() {
    if (!ready) {
      pipeline.setState(org.freedesktop.gstreamer.State.READY);
      newFrame = false;
      ready = true;
    }
  }


  private void makeBusConnections(Bus bus) {
    bus.connect(new Bus.ERROR() {
      public void errorMessage(GstObject arg0, int arg1, String arg2) {
        System.err.println(arg0 + " : " + arg2);
      }
    });
    bus.connect(new Bus.EOS() {
      public void endOfStream(GstObject arg0) {
        try {
          stop();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
  }


  ////////////////////////////////////////////////////////////

  // Stream event handling.


  private void seek(double rate, long start, long stop) {
    Gst.invokeLater(new Runnable() {
      public void run() {
        boolean res = pipeline.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, start, SeekType.SET, stop);
        if (!res) {
          PGraphics.showWarning("Seek operation failed.");
        }
      }
    });
  }


  private void fireCaptureEvent() {
    if (captureEventMethod != null) {
      try {
        captureEventMethod.invoke(eventHandler, this);
      } catch (Exception e) {
        System.err.println("error, disabling captureEvent()");
        e.printStackTrace();
        captureEventMethod = null;
      }
    }
  }


  ////////////////////////////////////////////////////////////

  // Buffer source interface.


  /**
   * Sets the object to use as destination for the frames read from the stream.
   * The color conversion mask is automatically set to the one required to
   * copy the frames to OpenGL.
   *
   * NOTE: This is not official API and may/will be removed at any time.
   *
   * @param Object dest
   */
  public void setBufferSink(Object sink) {
    bufferSink = sink;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public boolean hasBufferSink() {
    return bufferSink != null;
  }


  /**
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public synchronized void disposeBuffer(Object buf) {
    ((Buffer)buf).dispose();
  }


  protected void getSinkMethods() {
    try {
      sinkCopyMethod = bufferSink.getClass().getMethod("copyBufferFromSource",
        new Class[] { Object.class, ByteBuffer.class, int.class, int.class });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have a " +
                                 "copyBufferFromSource method.");
    }

    try {
      sinkSetMethod = bufferSink.getClass().getMethod("setBufferSource",
        new Class[] { Object.class });
      sinkSetMethod.invoke(bufferSink, new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have a " +
                                 "setBufferSource method.");
    }

    try {
      sinkDisposeMethod = bufferSink.getClass().getMethod("disposeSourceBuffer",
        new Class[] { });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have " +
                                 "a disposeSourceBuffer method.");
    }

    try {
      sinkGetMethod = bufferSink.getClass().getMethod("getBufferPixels",
        new Class[] { int[].class });
    } catch (Exception e) {
      throw new RuntimeException("Capture: provided sink object doesn't have " +
                                 "a getBufferPixels method.");
    }
  }


  public synchronized void post() {
    if (useBufferSink && sinkDisposeMethod != null) {
      try {
        sinkDisposeMethod.invoke(bufferSink, new Object[] {});
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   *  Returns a list of all capture devices, using the device's pretty display name.
   *  Multiple devices can have identical display names, appending ' #n' to devices
   *  with duplicate display names.
   *  @return array of device names
   *  @webref capture
   *  @webBrief Get a list of all capture device names
   */
  static public String[] list() {
    Video.init();

    String[] out;

    DeviceMonitor monitor = new DeviceMonitor();
    monitor.addFilter("Video/Source", null);
    devices = monitor.getDevices();
    monitor.close();

    out = new String[devices.size()];
    for (int i = 0; i < devices.size(); i++) {
      Device dev = devices.get(i);
      out[i] = checkCameraDuplicates(dev) > 1 ? assignDisplayName(dev, i) : dev.getDisplayName();
    }

    return out;
  }

  // This is a temporary addition until it's decided how to bring back resolution/framerate caps to the official API.
  // The old way of doing things is still listed in the video tutorial:
  // https://processing.org/tutorials/video
  static public String[] getCapabilities(String device) {
    for (int i=0; i < devices.size(); i++) {
      String deviceName = assignDisplayName(devices.get(i), i);
      if (devices.get(i).getDisplayName().equals(device) || devices.get(i).getName().equals(device) || deviceName.equals(device)) {
        return parseCaps(devices.get(i));
      }
    }
    return new String[]{};
  }

  static private String[] parseCaps(Device dev) {
    String[] caps = dev.getCaps().toString().split(";");
    ArrayList<String> devCaps = new ArrayList<String>();

    for (String cap: caps) {
      if (cap.indexOf("video/x-raw,") == -1) continue; // Looking for raw caps (excluding GLMemory stuff)

      int indexWidth = cap.indexOf("width");
      int indexHeight = cap.indexOf("height");
      int indexFramerate = cap.indexOf("framerate");

      String stringWidth = "";
      String stringHeight = "";
      String stringFramerate = "";

      if (0 < indexWidth && 0 < indexHeight && 0 < indexFramerate) {
        stringWidth = cap.substring(indexWidth, cap.indexOf(',', indexWidth));
        stringHeight = cap.substring(indexHeight, cap.indexOf(", format", indexHeight));
        stringFramerate = cap.substring(indexFramerate, cap.indexOf(']', indexFramerate));
      }
//      PApplet.println("=======>", cap);
      if (0 < stringHeight.indexOf("{")) {
        // A list of heights... something like "height=(int){ 448, 600 }
        stringHeight = stringHeight.substring(13, stringHeight.length() - 1);
        String[] values = stringHeight.split(",");
        for (String value: values) {
          stringHeight = "height=(int)" + value.trim();
          addCapStringsToList(stringWidth, stringHeight, stringFramerate, devCaps);
        }
      } else {
        addCapStringsToList(stringWidth, stringHeight, stringFramerate, devCaps);
      }
    }

    String[] out = new String[0];
    return devCaps.toArray(out);
  }

  static private void addCapStringsToList(String stringWidth, String stringHeight, String stringFramerate, ArrayList<String> devCaps) {
    if (0 < stringWidth.split("=").length) { // Expecting a string of the form "width=(int)1600"
      stringWidth = stringWidth.substring(11);
      try {
        Integer.parseInt(stringWidth);
      } catch (NumberFormatException ex) {
        stringHeight = "";
      }
    }
    if (0 < stringHeight.split("=").length) { // Expecting a string of the form "height=(int)896"
      stringHeight = stringHeight.substring(12);
      try {
        Integer.parseInt(stringHeight);
      } catch (NumberFormatException ex) {
        stringHeight = "";
      }
    }
    if (0 < stringFramerate.split("=,").length) { // Expecting a string of the form "framerate=(fraction)[ 5/1, 10000000/333333"
      stringFramerate = stringFramerate.substring(stringFramerate.indexOf("="));
      String[] fpsParts = stringFramerate.split(",");
      if (1 < fpsParts.length) {
        stringFramerate = fpsParts[1].trim();
        fpsParts = stringFramerate.split("/");
        if (fpsParts.length == 2) {
          try {
            int fpsNumerator = Integer.parseInt(fpsParts[0]);
            int fpsDenominator = Integer.parseInt(fpsParts[1]);
            int fps = fpsNumerator / fpsDenominator;
            stringFramerate = String.valueOf(fps);
          } catch (NumberFormatException ex) {
            stringFramerate = "";
          }
        }
      }
    }
    if (!stringWidth.equals("") && !stringHeight.equals("") && !stringFramerate.equals("")) {
      devCaps.add("size=" + stringWidth + "x" + stringHeight + ",fps=" + stringFramerate);
    }
  }

  static private String assignDisplayName(Device d, int pos) {
	  String s = "";
	  int count = 1;

	  for(int i = 0; i < devices.size(); i++) {
		  if (devices.get(i).getDisplayName().equals(d.getDisplayName())){
			  if (i == pos) {
				  s = d.getDisplayName() + " #" + Integer.toString(count);
			  }
			  count++;
		  }
	  }

	  return s;
  }

  static private int checkCameraDuplicates(Device d) {
	  int count = 0;
	  for (int i = 0; i < devices.size(); i++) {
		  if (devices.get(i).getDisplayName().equals(d.getDisplayName())) {
			  count++;
		  }
	  }
	  return count;
  }


  private class NewSampleListener implements AppSink.NEW_SAMPLE {

    @Override
    public FlowReturn newSample(AppSink sink) {
      Sample sample = sink.pullSample();

      // Pull out metadata from caps
      Structure capsStruct = sample.getCaps().getStructure(0);
      sourceWidth = capsStruct.getInteger("width");
      sourceHeight = capsStruct.getInteger("height");
      Fraction fps = capsStruct.getFraction("framerate");
      sourceFrameRate = (float)fps.numerator / fps.denominator;

      // Set the playback rate to the file's native framerate
      // unless the user has already set a custom one
      if (frameRate == -1.0) {
        frameRate = sourceFrameRate;
      }

      Buffer buffer = sample.getBuffer();
      ByteBuffer bb = buffer.map(false);
      if (bb != null) {

        // If the EDT is still copying data from the buffer, just drop this frame
        if (!bufferLock.tryLock()) {
          return FlowReturn.OK;
        }

        available = true;
        if (useBufferSink && bufferSink != null) { // The native buffer from GStreamer is copied to the buffer sink.

          try {
            sinkCopyMethod.invoke(bufferSink, new Object[] { buffer, bb, sourceWidth, sourceHeight });
            if (capturing) {
              fireCaptureEvent();
            }
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            bufferLock.unlock();
          }

        } else {
          IntBuffer rgb = bb.asIntBuffer();

          if (copyPixels == null) {
            copyPixels = new int[sourceWidth * sourceHeight];
          }

          try {
            rgb.get(copyPixels, 0, width * height);
            if (capturing) {
              fireCaptureEvent();
            }
          } finally {
            bufferLock.unlock();
          }

        }

        buffer.unmap();
      }
      sample.dispose();
      return FlowReturn.OK;
    }
  }


  private class NewPrerollListener implements AppSink.NEW_PREROLL {
    @Override
    public FlowReturn newPreroll(AppSink sink) {
      Sample sample = sink.pullPreroll();

      // Pull out metadata from caps
      Structure capsStruct = sample.getCaps().getStructure(0);
      sourceWidth = capsStruct.getInteger("width");
      sourceHeight = capsStruct.getInteger("height");
      Fraction fps = capsStruct.getFraction("framerate");
      sourceFrameRate = (float)fps.numerator / fps.denominator;

      // Set the playback rate to the file's native framerate
      // unless the user has already set a custom one
      if (frameRate == -1.0) {
        frameRate = sourceFrameRate;
      }

      sample.dispose();
      return FlowReturn.OK;
    }
  }
}
