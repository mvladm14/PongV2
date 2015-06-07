package inria.pongv2.activities;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import inria.pongv2.R;
import inria.pongv2.models.ParcelableBallCoordinates;
import inria.pongv2.services.DownloadResultReceiver;
import inria.pongv2.services.DownloadService;


public class MainActivity extends Activity implements SensorEventListener, DownloadResultReceiver.Receiver {

    public static final String RECEIVER = "RECEIVER";
    public static final String PHONE_COORDS = "PHONE_COORDS";
    public static final String BALL_COORDS = "BALL COORDS";

    private TextView tv;
    private TextView tv2;
    private SensorManager sManager;
    private Intent mIntent;
    private float[] mGravity;
    private float[] mGeomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("ok");
        /* Starting Download Service */
        DownloadResultReceiver mReceiver = new DownloadResultReceiver(new Handler());
        mReceiver.setReceiver(this);

        mIntent = new Intent(Intent.ACTION_SYNC, null, this, DownloadService.class);
        mIntent.putExtra(RECEIVER, mReceiver);

        tv = (TextView) findViewById(R.id.textView3);
        tv2 = (TextView) findViewById(R.id.textView4);
        //get a hook to the sensor service
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //when this Activity starts
    @Override
    protected void onResume() {
        super.onResume();
        /*register the sensor listener to listen to the gyroscope sensor, use the
        callbacks defined in this class, and gather the sensor information as quick
        as possible*/
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);
        sManager.registerListener(this, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_UI);
    }

    //When this Activity isn't visible anymore
    @Override
    protected void onStop() {
        //unregister the sensor listener
        sManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case DownloadService.STATUS_RUNNING:

                setProgressBarIndeterminateVisibility(true);
                break;
            case DownloadService.STATUS_FINISHED:
                /* Hide progress & extract result from bundle */
                setProgressBarIndeterminateVisibility(false);

                ParcelableBallCoordinates coordinates = resultData.getParcelable(BALL_COORDS);
                tv.setText(coordinates.toString());

                break;
            case DownloadService.STATUS_ERROR:
                /* Handle the error */
                String error = resultData.getString(Intent.EXTRA_TEXT);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // at this point, orientation contains the azimut,
                // pitch and roll values.

                //Convert to degrees
                for (int i=0; i < orientation.length; i++) {
                    Double degrees = (orientation[i] * 180) / Math.PI;
                    orientation[i] = degrees.floatValue();
                }

                mIntent.putExtra(PHONE_COORDS, orientation);

                startService(mIntent);

                /*
                //Convert to degrees
                for (int i=0; i < orientation.length; i++) {
                    Double degrees = (orientation[i] * 180) / Math.PI;
                    orientation[i] = degrees.floatValue();
                */
                tv2.setText("azimut = " + String.format("%.6f",orientation[0]) +
                        "\npitch = " + String.format("%.6f",orientation[1]) +
                        "\nroll = " + String.format("%.6f",orientation[2]));


            }
        }
    }

    /*
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                return;
            }

            tv2.setText("x = " + String.format("%.3f",sensorEvent.values[0]) +
                        "\ny= " + String.format("%.3f",sensorEvent.values[1]) +
                        "\nz= " + String.format("%.3f",sensorEvent.values[2]));


            mIntent.putExtra(PHONE_COORDS, sensorEvent.values);

            //startService(mIntent);
        }
    */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
