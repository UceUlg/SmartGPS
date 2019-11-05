package be.uliege.uce.smartgps.utilities;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class DetectNoise{

    private MediaRecorder mRecorder = null;
    private double mEMA;

    public void start(Context context) {

        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            if (mRecorder == null) {

                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");

                try {
                    mRecorder.prepare();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mRecorder.start();
                mEMA = 0.0;
            }
        }else {
            System.out.println("///////////////////////////////");
            System.out.println("mar");
            System.out.println("///////////////////////////////");
        }
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return   20 * Math.log10((mRecorder.getMaxAmplitude()/51805.5336) / 0.00002);
        else
            return 0;
    }

    public static boolean hasPermissions(Context context, String...permissions){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M &&context!=null&& permissions!=null){
            for (String permission:permissions){
                if(ActivityCompat.checkSelfPermission(context,permission)!= PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }
}
