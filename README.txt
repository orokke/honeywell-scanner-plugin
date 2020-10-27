*******************************************************************************
README.TXT
*******************************************************************************
UNLESS OTHERWISE AGREED TO IN A SIGNED WRITING BY HONEYWELL INTERNATIONAL INC
("HONEYWELL") AND THE USER OF THIS CODE, THIS CODE AND INFORMATION IS PROVIDED
"AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
FOR A PARTICULAR PURPOSE.

COPYRIGHT (C) 2016 HONEYWELL INTERNATIONAL INC.
 
THIS SOFTWARE IS PROTECTED BY COPYRIGHT LAWS OF THE UNITED STATES OF
AMERICA AND OF FOREIGN COUNTRIES. THIS SOFTWARE IS FURNISHED UNDER A
LICENSE AND/OR A NONDISCLOSURE AGREEMENT AND MAY BE USED IN ACCORDANCE
WITH THE TERMS OF THOSE AGREEMENTS. UNAUTHORIZED REPRODUCTION,  DUPLICATION
OR DISTRIBUTION OF THIS SOFTWARE, OR ANY PORTION OF IT  WILL BE PROSECUTED
TO THE MAXIMUM EXTENT POSSIBLE UNDER THE LAW.
******************************************************************************* 
UNSUPPORTED SAMPLE CODE that implements a Codova plugin for reading barcodes
with the Honeywell Data Collection SDK for Android
*******************************************************************************

-------------------
CREATING SAMPLE APP
-------------------

The following steps explain how to create a sample application for Android that
can be used to test the sample Codova plugin for reading barcodes with the 
Honeywell Data Collection SDK for Android. This instructions assume the following
directory structure for your Cordova application and plug-ins source code.

    <rootdir>\Apps
    <rootdir>\Plugins

where <rootdir> is wherever you want to create the sample app e.g. C:\Source

Copy plug-in files into <rootdir>\Plugins\BarcodeReader:

    <rootdir>\PlugIns\BarcodeReader\plugin.xml
    <rootdir>\PlugIns\BarcodeReader\src\android\BarcodeReaderPlugin.java
    <rootdir>\PlugIns\BarcodeReader\src\android\libs\DataCollection.jar
    <rootdir>\PlugIns\BarcodeReader\www\BarcodeReaderPlugin.js

Open command prompt and go to Apps directory:

    cd <rootdir>\Apps

Create default Cordova application called BarcodeReaderTest:

    cordova create BarcodeReaderTest com.example.barcodereadertest BarcodeReaderTest

Copy test app files into <rootdir>Apps\BarcodeReaderTest, replacing the default ones created by Cordova:

    <rootdir>\Apps\BarcodeReaderTest\config.xml
    <rootdir>\Apps\BarcodeReaderTest\www\index.html
    <rootdir>\Apps\BarcodeReaderTest\www\css\index.css
    <rootdir>\Apps\BarcodeReaderTest\www\js\index.js

Go into BarcodeReaderTest directory:

    cd BarcodeReaderTest
    
Add Android platform files to app:

    cordova platform add android

Add BarcodeReaderPlugin plug-in files to app:

    cordova plugin add ..\..\Plugins\BarcodeReader

Build the app

    cordova build

----------------------
MODIFYING PLUG-IN CODE
----------------------

Modifications to the plug-in should be made to the master copy in <rootdir>\Plugins\BarcodeReader.
To copy those modifications into the sample app again, remove and add the plugin again e.g.

    cordova plugin remove BarcodeReaderPlugin
    cordova plugin add ..\..\PlugIns\BarcodeReader

------------------
RUNNING SAMPLE APP
------------------

Before running application, you may want to start a logcat session to watch for messages from
plug-in and chromium (which is hosting the Codova app). You can use the following adb filter to do this:

    adb logcat BarcodeReaderPlugin:V chromium:V *:S
    
Run test app in the usual way e.g. run this command from the <rootdir>\Apps\BarcodeReaderTest directory:

    cordova run
    
When sample application starts, you will see three buttons and two text areas. Start and Stop buttons can
be used to start (BarcodeReaderPlugin.createReader) and stop (BarcodeReaderPlugin.closeReader) barcode
reading via the plug-in.

When barcode reader is started, scanned barcode data will appear in the top read-only text area regardless
of where the text focus is. When barcoder reader is not started or stopped again, data will be wedged into
app in the usual way, assuming you haven't disabled the wedge. You can use the lower text area to test
this behaviour.

Note that the barcode reader plugin performs neccessary clean up of the underlying SDK objects even if
the app exits without first calling closeReader() e.g. if user presses Back button. See the onDestroy()
method override in the plug-in Java code.

The Set Properties button will set some properties that are different from defaults e.g. disable Code 39.
You can test how the barcode scanner behaves before and after setting properties.
