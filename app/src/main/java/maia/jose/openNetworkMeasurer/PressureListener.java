package maia.jose.openNetworkMeasurer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class PressureListener implements SensorEventListener {
    private final String LOG_TAG = PressureListener.class.getSimpleName();
    public float pressure_value = 0.0f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        pressure_value = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
