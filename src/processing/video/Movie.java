/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
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

import java.io.*;
import java.net.URI;
import java.nio.*;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.*;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.elements.*;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.event.SeekType;


/**
 * Datatype for storing and playing movies. Movies must be located in the sketch's data folder 
 * or an accessible place on the network to load without an error.
 *
 * @webref video
 * @usage application
 */
public class Movie extends PImage implements PConstants {
  public static String[] supportedProtocols = { "http" };

  public String filename;
  public PlayBin playbin;
  
  // The source resolution and framerate of the file
  public int sourceWidth;
  public int sourceHeight;
  public float sourceFrameRate;
  
  public float frameRate;             // the current playback fps  
  protected float rate;               // speed multiplier (1.0: frameRate = nativeFrameRate)

  protected float volume;
  
  protected boolean playing = false;
  protected boolean paused = false;
  protected boolean repeat = false;

  protected Method movieEventMethod;
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

  private NewSampleListener newSampleListener;
  private NewPrerollListener newPrerollListener;
  private final Lock bufferLock = new ReentrantLock();
  

  /**
   * Creates an instance of Movie loading the movie from filename.
   *
   * @param parent PApplet
   * @param filename String
   */
  public Movie(PApplet parent, String filename) {
    super(0, 0, RGB);
    initGStreamer(parent, filename);
  }


  /**
   * Disposes all the native resources associated to this movie.
   * 
   * NOTE: This is not official API and may/will be removed at any time.
   */
  public void dispose() {
    if (playbin != null) {
      try {
        if (playbin.isPlaying()) {
          playbin.stop();
          playbin.getState();
        }
      } catch (Exception e) {
      }

      pixels = null;

      rgbSink.disconnect(newSampleListener);
      rgbSink.disconnect(newPrerollListener);
      rgbSink.dispose();
      playbin.setState(org.freedesktop.gstreamer.State.NULL);
      playbin.getState();
      playbin.getBus().dispose();
      playbin.dispose();
      
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
      super.finalize();
    }
  }


  /**
   * Sets how often frames are read from the movie. Setting the <b>fps</b>
   * parameter to 4, for example, will cause 4 frames to be read per second.
   *
   * @webref movie
   * @usage web_application
   * @param ifps speed of the movie in frames per second
   * @brief Sets the target frame rate
   */
  public void frameRate(float ifps) {
    // We calculate the target ratio in the case both the
    // current and target framerates are valid (greater than
    // zero), otherwise we leave it as 1.
    float f = (0 < ifps && 0 < frameRate) ? ifps / frameRate : 1;
    
    long t = playbin.queryPosition(TimeUnit.NANOSECONDS);
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
   * Sets the relative playback speed of the movie. The <b>rate</b>
   * parameters sets the speed where 2.0 will play the movie twice as fast,
   * 0.5 will play at half the speed, and -1 will play the movie in normal
   * speed in reverse.
   *
   * @webref movie
   * @usage web_application
   * @param irate speed multiplier for movie playback
   * @brief Sets the relative playback speed
   */
  public void speed(float irate) {
    // If the frameRate() method is called continuously with very similar
    // rate values, playback might become sluggish. This condition attempts
    // to take care of that.
    if (PApplet.abs(rate - irate) > 0.1) {
      rate = irate;
      frameRate(frameRate); // The framerate is the same, but the rate (speed) could be different.
    }
  }


  /**
   * Returns the length of the movie in seconds. If the movie is 1 minute and
   * 20 seconds long the value returned will be 80.0.
   *
   * @webref movie
   * @usage web_application
   * @brief Returns length of movie in seconds
   */
  public float duration() {
    long nanosec = playbin.queryDuration(TimeUnit.NANOSECONDS);
    return Video.nanoSecToSecFrac(nanosec);    
  }


  /**
   * Returns the location of the playback head in seconds. For example, if
   * the movie has been playing for 4 seconds, the number 4.0 will be returned.
   *
   * @webref movie
   * @usage web_application
   * @brief Returns location of playback head in units of seconds
   */
  public float time() {
    long nanosec = playbin.queryPosition(TimeUnit.NANOSECONDS);
    return Video.nanoSecToSecFrac(nanosec);
  }


  /**
   * Jumps to a specific location within a movie. The parameter <b>where</b>
   * is in terms of seconds. For example, if the movie is 12.2 seconds long,
   * calling <b>jump(6.1)</b> would go to the middle of the movie.
   *
   * @webref movie
   * @usage web_application
   * @param where position to jump to specified in seconds
   * @brief Jumps to a specific location
   */
  public void jump(float where) {
    setReady();

    // Round the time to a multiple of the source framerate, in
    // order to eliminate stutter. Suggested by Daniel Shiffman
    if (sourceFrameRate != -1) {
      int frame = (int)(where * sourceFrameRate);
      where = frame / sourceFrameRate;
    }

    long pos = Video.secToNanoLong(where);
    seek(rate, pos, -1);
  }


  /**
   * Returns "true" when a new movie frame is available to read.
   *
   * @webref movie
   * @usage web_application
   * @brief Returns "true" when a new movie frame is available to read.
   */
  public boolean available() {
    return available;
  }


  /**
   * Plays a movie one time and stops at the last frame.
   *
   * @webref movie
   * @usage web_application
   * @brief Plays movie one time and stops at the last frame
   */
  public void play() {
    setReady();
    
    playbin.play();
    playbin.getState();    
    
    playing = true;
    paused = false;
  }


  /**
   * Plays a movie continuously, restarting it when it's over.
   *
   * @webref movie
   * @usage web_application
   * @brief Plays a movie continuously, restarting it when it's over.
   */
  public void loop() {
    repeat = true;
    play();
  }


  /**
   * If a movie is looping, calling noLoop() will cause it to play until the
   * end and then stop on the last frame.
   *
   * @webref movie
   * @usage web_application
   * @brief Stops the movie from looping
   */
  public void noLoop() {
    setReady();

    repeat = false;
  }


  /**
   * Pauses a movie during playback. If a movie is started again with play(),
   * it will continue from where it was paused.
   *
   * @webref movie
   * @usage web_application
   * @brief Pauses the movie
   */
  public void pause() {
    setReady();

    playbin.pause();
    playbin.getState();    

    playing = false;
    paused = true;    
  }


  /**
   * Stops a movie from continuing. The playback returns to the beginning so
   * when a movie is played, it will begin from the beginning.
   *
   * @webref movie
   * @usage web_application
   * @brief Stops the movie
   */
  public void stop() {
    setReady();

    playbin.stop();
    playbin.getState();    
    
    playing = false;
    paused = false;    
  }


  /**
   * Reads the current frame of the movie.
   *
   * @webref movie
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
        Object cache = parent.g.getCache(Movie.this);
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
   * Change the volume. Values are from 0 to 1.
   *
   * @param float v
   */
  public void volume(float v) {
    if (playing && PApplet.abs(volume - v) > 0.001f) {

      playbin.setVolume(v);
      playbin.getState();      
      
      volume = v;
    }
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


  @Override
  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    if (outdatedPixels) loadPixels();
    super.getImpl(sourceX, sourceY, sourceWidth, sourceHeight,
    target, targetX, targetY);
  }
  
  
  /**
   * Check if this movie object is currently playing.
   */
  public boolean isPlaying() {
    return playing;
  }


  /**
   * Check if this movie object is currently paused.
   */
  public boolean isPaused() {
    return paused;
  }


  /**
   * Check if this movie object is currently looping.
   */
  public boolean isLooping() {
    return repeat;
  }
  
  
  ////////////////////////////////////////////////////////////

  // Initialization methods.


  protected void initGStreamer(PApplet parent, String filename) {
    this.parent = parent;

    Video.init();    
    playbin = null;
    
    File file;
    
    // First check to see if this can be read locally from a file.
    try {      
      try {
        // Try a local file using the dataPath. usually this will
        // work ok, but sometimes the dataPath is inside a jar file,
        // which is less fun, so this will crap out.
        file = new File(parent.dataPath(filename));
        if (file.exists()) {
          playbin = new PlayBin("Movie Player");
          playbin.setInputFile(file);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Read from a file just hanging out in the local folder.
      // this might happen when the video library is used with some
      // other application, or the person enters a full path name
      if (playbin == null) {
        try {
          file = new File(filename);
          if (file.exists()) {
            playbin = new PlayBin("Movie Player");
            playbin.setInputFile(file);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (playbin == null) {
        // Try network read...
        for (int i = 0; i < supportedProtocols.length; i++) {
          if (filename.startsWith(supportedProtocols[i] + "://")) {
            try {
              playbin = new PlayBin("Movie Player");
              playbin.setURI(URI.create(filename));
              break;
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    } catch (SecurityException se) {
      // online, whups. catch the security exception out here rather than
      // doing it three times (or whatever) for each of the cases above.
    }

    if (playbin == null) {
      parent.die("Could not load movie file " + filename, null);
    }

    initSink();
    
    playbin.setVideoSink(rgbSink);
    makeBusConnections(playbin.getBus());    
    
    // We've got a valid movie! let's rock.
    try {
      this.filename = filename; // for error messages

      // register methods
      parent.registerMethod("dispose", this);
      parent.registerMethod("post", this);

      setEventHandlerObject(parent);

      sourceWidth = sourceHeight = 0;
      sourceFrameRate = -1;
      frameRate = -1;
      rate = 1.0f;
      volume = -1;
      ready = false;      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Uses a generic object as handler of the movie. This object should have a
   * movieEvent method that receives a Movie argument. This method will
   * be called upon a new frame read event.
   *
   */
  protected void setEventHandlerObject(Object obj) {
    eventHandler = obj;

    try {
      movieEventMethod = eventHandler.getClass().getMethod("movieEvent", Movie.class);
      return;
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }

    // movieEvent can alternatively be defined as receiving an Object, to allow
    // Processing mode implementors to support the video library without linking
    // to it at build-time.
    try {
      movieEventMethod = eventHandler.getClass().getMethod("movieEvent", Object.class);
    } catch (Exception e) {
      // no such method, or an error... which is fine, just ignore
    }
  }


  protected void initSink() {        
    rgbSink = new AppSink("movie sink");
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
      playbin.setState(org.freedesktop.gstreamer.State.READY); 
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
        if (repeat) {
          if (0 < rate) {
            // Playing forward, so we return to the beginning
            jump(0);
          } else {
            // Playing backwards, so we go to the end.
            jump(duration());
          }

          // The rate is set automatically to 1 when restarting the
          // stream, so we need to call frameRate in order to reset
          // to the latest fps rate.
          frameRate(frameRate);
        } else {
          playing = false;
        }
      }
    });
  }


  
  ////////////////////////////////////////////////////////////

  // Stream event handling.


  private void seek(double rate, long start, long stop) {
    Gst.invokeLater(new Runnable() {
      public void run() {
        boolean res;
        if (stop == -1) {
          res = playbin.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, start, SeekType.NONE, stop);
        } else {
          res = playbin.seek(rate, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), SeekType.SET, start, SeekType.SET, stop);  
        }
        if (!res) {
          PGraphics.showWarning("Seek operation failed.");
        }
      }
    });    
  }
  
  
  private void fireMovieEvent() {
    if (movieEventMethod != null) {
      try {
        movieEventMethod.invoke(eventHandler, this);
      } catch (Exception e) {
        System.err.println("error, disabling movieEvent() for " + filename);
        e.printStackTrace();
        movieEventMethod = null;
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
      throw new RuntimeException("Movie: provided sink object doesn't have a " +
                                 "copyBufferFromSource method.");
    }

    try {
      sinkSetMethod = bufferSink.getClass().getMethod("setBufferSource",
        new Class[] { Object.class });
      sinkSetMethod.invoke(bufferSink, new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have a " +
                                 "setBufferSource method.");
    }
    
    try {
      sinkDisposeMethod = bufferSink.getClass().getMethod("disposeSourceBuffer", 
        new Class[] { });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have " +
                                 "a disposeSourceBuffer method.");
    }
        
    try {
      sinkGetMethod = bufferSink.getClass().getMethod("getBufferPixels", 
        new Class[] { int[].class });
    } catch (Exception e) {
      throw new RuntimeException("Movie: provided sink object doesn't have " +
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
  
  
  ////////////////////////////////////////////////////////////

  // Listener of GStreamer events.
  
  
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
            if (playing) {
              fireMovieEvent();
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
            if (playing) {
              fireMovieEvent();
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
