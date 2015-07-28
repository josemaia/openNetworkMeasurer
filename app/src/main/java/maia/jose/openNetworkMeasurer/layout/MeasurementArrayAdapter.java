package maia.jose.openNetworkMeasurer.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import maia.jose.openNetworkMeasurer.R;

public class MeasurementArrayAdapter extends ArrayAdapter<MeasurementEntry> {
    private final Context context;

    public MeasurementArrayAdapter(Context context) {
        super(context, R.layout.measurement_listitem);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MeasurementHolder holder = new MeasurementHolder();
        View v = convertView;

        if (convertView == null) { // new view
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.measurement_listitem, parent, false);

            TextView title = (TextView) v.findViewById(R.id.measurement_title);
            TextView subtitle = (TextView) v.findViewById(R.id.measurement_subtitle);

            holder.titleView = title;
            holder.subtitleView = subtitle;

            v.setTag(holder);
        }
        else{
            holder = (MeasurementHolder) v.getTag();
        }
        final MeasurementEntry entry = getItem(position);
        holder.titleView.setText(entry.getTitle());
        holder.subtitleView.setText(entry.getSubtitle());

        return v;
    }


    private static class MeasurementHolder{
        public TextView titleView;
        public TextView subtitleView;
    }
}

