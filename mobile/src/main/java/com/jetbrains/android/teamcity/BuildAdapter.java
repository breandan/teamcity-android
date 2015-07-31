package com.jetbrains.android.teamcity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jetbrains.android.teamcity.rest.Builds;

import java.util.List;

/**
 * Created by breandan on 7/30/2015.
 */
public class BuildAdapter extends ArrayAdapter<Builds.Build> {

    private final int layoutResourceId;
    private final Context context;
    private final List<Builds.Build> data;

    public BuildAdapter(Context context, int layoutResourceId, List<Builds.Build> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listview_item_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.txtTitle);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imgIcon);

        String text = data.get(position).toString();
        textView.setText(text);

        if (text.contains("SUCCESS")) {
            textView.setBackgroundColor(Color.parseColor("#66FF66"));
        } else if (text.contains("FAILURE")) {
            textView.setBackgroundColor(Color.parseColor("#FF6666"));
        } else {
            textView.setBackgroundColor(Color.parseColor("#FFFF66"));
        }
        imageView.setImageResource(R.drawable.build_icon);

        return rowView;
    }

    static class WeatherHolder {
        ImageView imgIcon;
        TextView txtTitle;
    }
}
