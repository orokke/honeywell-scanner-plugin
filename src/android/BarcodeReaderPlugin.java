/*
 * UNLESS OTHERWISE AGREED TO IN A SIGNED WRITING BY HONEYWELL INTERNATIONAL INC
 * ("HONEYWELL") AND THE USER OF THIS CODE, THIS CODE AND INFORMATION IS PROVIDED
 * "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * COPYRIGHT (C) 2016 HONEYWELL INTERNATIONAL INC.
 * 
 * THIS SOFTWARE IS PROTECTED BY COPYRIGHT LAWS OF THE UNITED STATES OF
 * AMERICA AND OF FOREIGN COUNTRIES. THIS SOFTWARE IS FURNISHED UNDER A
 * LICENSE AND/OR A NONDISCLOSURE AGREEMENT AND MAY BE USED IN ACCORDANCE
 * WITH THE TERMS OF THOSE AGREEMENTS. UNAUTHORIZED REPRODUCTION,  DUPLICATION
 * OR DISTRIBUTION OF THIS SOFTWARE, OR ANY PORTION OF IT  WILL BE PROSECUTED
 * TO THE MAXIMUM EXTENT POSSIBLE UNDER THE LAW.
 */
 
/*
 * UNSUPPORTED SAMPLE CODE that implements a Codova plugin for reading barcodes
 * with the Honeywell Data Collection SDK for Android
 */
 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.util.concurrent.CountDownLatch;
import java.util.Map;
import java.util.HashMap;

import com.honeywell.aidc.*;

public class BarcodeReaderPlugin extends CordovaPlugin implements BarcodeReader.BarcodeListener {
 
    public static final String TAG = "BarcodeReaderPlugin";
    
    // Actions supported by our execute() method
    // These strings need to match the actions sent in exec() calls in Barcode.js
    private static final String CREATE_READER           = "createReader";
    private static final String ADD_BARCODE_LISTENER    = "addBarcodeListener";
    private static final String REMOVE_BARCODE_LISTENER = "removeBarcodeListener";
    private static final String SET_PROPERTIES          = "setProperties";
    private static final String CLOSE_READER            = "closeReader";
    
    private AidcManager manager;
    private BarcodeReader barcodeReader;
    private CallbackContext barcodeListenerCallbackContext;
         
    public BarcodeReaderPlugin() {
        manager = null;
        barcodeReader = null;
        barcodeListenerCallbackContext = null;
    }
    
    //
    // CordovaPlugin methods
    // see https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
    //
    
    /**
     * Called after plugin construction and fields have been initialized.
     */
    @Override
    protected void pluginInitialize() {
        Log.v(TAG,"pluginInitialize");
    }
    
    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {         
        Log.v(TAG,"execute: "+ action);
        
        if (action.equals(CREATE_READER)) {
            createReader(callbackContext);
        } else if (action.equals(ADD_BARCODE_LISTENER)) {
            addBarcodeListener(callbackContext);
        } else if (action.equals(REMOVE_BARCODE_LISTENER)) {
            removeBarcodeListener(callbackContext);
        } else if (action.equals(SET_PROPERTIES)) {
            setProperties(callbackContext, args);
        } else if (action.equals(CLOSE_READER)) {
            closeReader(callbackContext);
        } else {
            Log.e(TAG, "execute: invalid action '" + action + "'");
            return false;
        }
        
        // Return code should indicate if action was valid or not, so we return true
        // unless action was not recognized, in which case we already returned false above.
        return true;
    }
    
    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking) {
        Log.v(TAG, "onPause");
        
        if (barcodeReader != null) {
            // Release reader when activity is paused so we don't get notifications
            Log.v(TAG, "onPause: releasing barcode reader");
            barcodeReader.release();
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking		Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        Log.v(TAG, "onResume");
        
        if (barcodeReader != null) {
            try {
                // Try to reclaim reader when activity is resumed
                Log.v(TAG, "onResume: claming barcode reader");
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                Log.e(TAG, "onResume: failed to claim barcode reader");
                //TODO: how should we handle this failure?
            }
        }
    }
    
    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy: start");
        
        // Ensure all objects are cleared up if we are being destroyed
        closeReader();
        
        Log.v(TAG, "onDestroy: end");
    }

    //
    // BarcodeReaderPlugin plugin actions
    //
    
    private void createReader(final CallbackContext callbackContext) {
        Log.v(TAG, "createReader: start");
        
        if ((manager != null) || (barcodeReader != null)) {
            Log.w(TAG, "createReader: AidcManager and/or BarcodeReader objects already created");
            callbackContext.error("reader already created");
            return;
        }
        
        // CreatedCallback.onCreated() callback occurs on different thread so use countdown latch to sync
        final CountDownLatch createCompleted = new CountDownLatch(1);
        
        // Create AIDC Manager object
        Log.v(TAG, "createReader: creating AidcManager");
        AidcManager.create(this.cordova.getActivity().getApplicationContext(), new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                Log.v(TAG, "createReader.CreatedCallback: start");
                manager = aidcManager;               
                Log.v(TAG, "createReader.CreatedCallback: end");
                createCompleted.countDown();
            }
        });
        
        // Wait here for onCreated() to complete
        try { createCompleted.await(); } catch(InterruptedException ex) {};
        Log.v(TAG, "createReader: AidcManager created");
                
        // Get BarcodeReader object from AIDC Manager
        Log.v(TAG, "createReader: creating BarcodeReader");
        barcodeReader = manager.createBarcodeReader();
        
        if (barcodeReader != null) {
            Log.v(TAG, "createReader: BarcodeReader created");
            
            try {
                // Set trigger mode to "auto" so we don't have to handle the trigger ourselves
                Log.v(TAG, "createReader: setting trigger property to auto");
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);                
            } catch(UnsupportedPropertyException ex) {
                Log.e(TAG, "createReader: failed to set trigger control to auto");
                closeReader();
                callbackContext.error("failed to set trigger control to auto");
                return;
            }
            
           try {
                // Claim the scanner now
                Log.v(TAG, "createReader: claiming barcode reader");
                barcodeReader.claim();
            } catch(ScannerUnavailableException ex) {
                Log.e(TAG, "createReader: failed to claim barcode reader");
                closeReader();
                callbackContext.error("failed to claim barcode reader");
                return;
            }
            
            Log.v(TAG, "createReader: successfully created and initialized barcode reader");
            callbackContext.success();
        }
        else
        {
            Log.e(TAG, "createReader: failed to create BarcodeReader object");
            callbackContext.error("failed to create barcode reader");
            return;
        }
        
        Log.v(TAG, "createReader: end");       
    }
    
    // Note in this case we are going to store callback context and use:
    // "success" callback for successful barcode reads (BarcodeListener.onBarcodeEvent)
    // "error"   callback for failed     barcode reads (BarcodeListener.onFailureEvent)
    private void addBarcodeListener(CallbackContext callbackContext) {
        Log.v(TAG, "addBarcodeListener");

        if (barcodeReader != null) {
            
            // We will limit ourselves to one barcode listener so remove any existing listener first
            if (barcodeListenerCallbackContext != null) {
                Log.v(TAG, "addBarcodeListener: removing old barcode listener");
                removeBarcodeListener();
            }

            if (callbackContext != null) {
                // Store callback context used to return barcode data or errors
                barcodeListenerCallbackContext = callbackContext;
                
                // Register for barcode read and failure events
                Log.v(TAG, "addBarcodeListener: adding barcode listener");
                barcodeReader.addBarcodeListener(this);
            }                
        }
        
        SendNoResult(callbackContext, true);        
    }
    
    private void removeBarcodeListener(CallbackContext callbackContext) {
        removeBarcodeListener();
        SendNoResult(callbackContext, false);
    }
    
    private void removeBarcodeListener() {
        Log.v(TAG, "removeBarcodeListener");

        if ((barcodeReader != null) && (barcodeListenerCallbackContext != null )) {
            // Unregister for barcode read events
            Log.v(TAG, "removeBarcodeListener: removing barcode listener");
            barcodeReader.removeBarcodeListener(this);
            
            // Remove stored callback context
            barcodeListenerCallbackContext = null;
        }        
    }
    
    private void setProperties(CallbackContext callbackContext, JSONArray args) {
        Log.v(TAG, "setProperties");
        
        // Return now if barcode reader object not created yet
        if (barcodeReader == null) {
            Log.e(TAG, "setProperties: barcodeReader is null");
            callbackContext.error("Barcode reader not created");
            return;
        }
        
        // We are expecting one or more name,value pairs so args.length should be an even number
        int length = args.length();
        Log.v(TAG, "setProperties: args.length=" + length);        
        if ((length % 2) != 0) {
            Log.e(TAG, "setProperties: odd number of elements in argument array");
            callbackContext.error("odd number of elements in argument array");
            return;
        }
        
        // Convert JSONArray into HashMap of pair,value elements representing properties to set
        Map<String, Object> properties = new HashMap<String, Object>();
        String propName;
        Object propValue;       
        try {
            for (int i=0; i<length; /*empty*/)
            {
                // Name should always be a string
                propName = args.getString(i++);
                Log.v(TAG, "setProperties: propName=" + propName);
                
                // Value could be boolean, integer or string
                propValue = args.get(i++);
                Log.v(TAG, "setProperties: propValue=" + propValue);
                
                // We don't currently expose trigger events so do not allow changes to trigger control mode
                if (propName.equals(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE)) {
                    Log.w(TAG, "PROPERTY_TRIGGER_CONTROL_MODE property ignored");
                    continue;
                }
                
                // Add name,value pair into map
                properties.put(propName, propValue);
                Log.v(TAG, "setProperties: property added to map");
            }
        } catch (JSONException ex) {
            Log.e(TAG, "error occurred converting JSONArray to HashMap - " + ex.getMessage());
            callbackContext.error("invalid elements in argument array");
            return;
        }
        
        // Apply properties using map
        // BarcodeReader.setProperties() seems to ignore invalid property names
        barcodeReader.setProperties(properties);
        Log.v(TAG, "setProperties: barcode reader properties set");
        callbackContext.success();
        return;
    }
    
    private void closeReader(CallbackContext callbackContext) {
        closeReader();        
        SendNoResult(callbackContext, false);        
    }
    
    private void closeReader() {
        Log.v(TAG, "closeReader: start");

        if (barcodeReader != null) {
            // Unregister barcode listener if we have one (sample app doesn't do this, but example in documentation does)
            removeBarcodeListener();
            
            // Release claim on barcode scanner (not sure if this is actually neccessary before close?)
            barcodeReader.release();

            // Close BarcodeReader to clean up resources
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // Close AidcManager to disconnect from the data collection service
            // Once closed the object can no longer be used
            manager.close();
            manager = null;
        }        

        Log.v(TAG, "closeReader: end");
    }

    //
    // BarcodeListener methods
    //
    
    // Called when a bar code label is successfully scanned
    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        if (event != null) {
            Log.v(TAG, "onBarcodeEvent: barcodeData=" + event.getBarcodeData());
            
            if (barcodeListenerCallbackContext != null)
            {
                PluginResult result = new PluginResult(PluginResult.Status.OK, event.getBarcodeData());
                result.setKeepCallback(true);
                barcodeListenerCallbackContext.sendPluginResult(result);
            }
        } else {
            Log.w(TAG, "onBarcodeEvent: (no data)");
        }
    }

    // Called when a bar code label is not successfully scanned e.g. user released scan button before reading barcode
    @Override
    public void onFailureEvent(BarcodeFailureEvent event) {
        if (event != null) {
            Log.v(TAG, "onFailureEvent: timestamp=" + event.getTimestamp());
            
            if (barcodeListenerCallbackContext != null)
            {
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, event.getTimestamp());
                result.setKeepCallback(true);
                barcodeListenerCallbackContext.sendPluginResult(result);
            }
        } else {
            Log.w(TAG, "onFailureEvent: (no data)");
        }
    }
    
    //
    // Helper functions
    //
    
    private void SendNoResult(CallbackContext callbackContext, boolean keepCallback)
    {
        // Return "no result" result - neither success nor failure callbacks will be called
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(keepCallback);
        callbackContext.sendPluginResult(pluginResult);
    }
}
