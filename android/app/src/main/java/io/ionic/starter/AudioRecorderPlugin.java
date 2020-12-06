package io.ionic.starter;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static android.Manifest.permission.ACCESS_MEDIA_LOCATION;
import static android.Manifest.permission.RECORD_AUDIO;
import static io.ionic.starter.AudioRecorderPlugin.REQUEST_AUDIORECORDER;

@NativePlugin(
        permissions = {
                ACCESS_MEDIA_LOCATION, RECORD_AUDIO
        },
        requestCodes = {
                REQUEST_AUDIORECORDER
        }
)
public class AudioRecorderPlugin extends Plugin {

    protected static final int REQUEST_AUDIORECORDER = 23451; // Unique request code
    protected static final int REQUEST_MEDIA_LOCATION = 23452; // Unique request code


    public void load() {
        // Called when the plugin is first constructed in the bridge
    }

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;

    private MediaRecorder recorder = null;
    private Integer duration = null;
    private String fullPath = "";
    private MediaPlayer   player = null;


    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void start(PluginCall call) {
        String fileName = call.getString("fileName");
        int duration;
        if (call.getInt("duration") != null) {
            duration = call.getInt("duration");
        } else {
            duration = 0;
        }

        // Record to the external cache directory for visibility
        fileName = getContext().getFilesDir().getAbsolutePath();
        fileName += "/audio-" + (new Date()).getTime() + ".mp3";
        fullPath = fileName;

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(16000);

        if (duration != 0) {
            //     recorder.setMaxDuration(duration);


            recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        recorder.stop();
                        recorder.release();
                        recorder = null;

                        JSObject ret = new JSObject();
                        ret.put("file", fullPath);
                        ret.put("duration", getMediaDuration());
                        notifyListeners("recordingFinished", ret);
                    }
                }
            });
        }

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AudioRecorder", "prepare() failed");
            call.reject(e.getMessage());
        }


        recorder.start();
        JSObject ret = new JSObject();
        ret.put("start", true);
        call.resolve(ret);
    }

    int getMediaDuration() {
        Uri uri = Uri.parse(fullPath);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(getContext(),uri);
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(durationStr);
    }

    @PluginMethod()
    public void stop(PluginCall call) {

        if ( recorder == null) {
            call.reject("No Active Recording");
            return;
        }

        recorder.stop();
        recorder.release();
        recorder = null;

        JSObject ret = new JSObject();
        ret.put("file", fullPath);
        ret.put("duration", getMediaDuration());

        notifyListeners("recordingFinished", ret);

        call.resolve(ret);
    }

    @PluginMethod()
    public void startPlaying(PluginCall call) {
        String fileName = call.getString("fileName");
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();

            JSObject ret = new JSObject();
            ret.put("startPlaying", true);
            call.resolve(ret);
        } catch (IOException e) {
            Log.e("AudioRecorder", "prepare() failed");
            call.reject(e.getMessage());
        }
    }

    @PluginMethod()
    public void deleteRecording(PluginCall call) {
        String fileName = call.getString("fileName");

        Log.e("AudioRecorder", "delete" + Uri.parse(fileName).toString());
        try {
            Boolean result = new File(fileName).getAbsoluteFile().delete();

            JSObject ret = new JSObject();
            ret.put("deleteRecording", result);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e("AudioRecorder", "deleteRecording() failed");
            call.reject(e.getMessage());
        }
    }

    @PluginMethod()
    public void stopPlaying(PluginCall call) {

        if ( player == null) {
            call.reject("No Active Playback");
            return;
        }

        player.release();
        player = null;

        JSObject ret = new JSObject();
        ret.put("stopPlaying", true);
        call.resolve(ret);
    }

    @PluginMethod()
    public void authorize(PluginCall call) {
        String value = call.getString("value");
        saveCall(call);
        pluginRequestPermission(RECORD_AUDIO, REQUEST_AUDIORECORDER);
        pluginRequestPermission(ACCESS_MEDIA_LOCATION, REQUEST_AUDIORECORDER);
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d("AudioRecorder", "No stored plugin call for permissions request result");
            return;
        }

        Log.d("AudioRecorder", permissions.toString());
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                Log.d("AudioRecorder", "User denied permission: ");
                return;
            }
        }

        if (requestCode == REQUEST_AUDIORECORDER) {
            // We got the permission!
            Log.d("AudioRecorder", "User HAS REQUEST_AUDIORECORDER, REQUEST_MEDIA_LOCATION permission");
        }

    }
}