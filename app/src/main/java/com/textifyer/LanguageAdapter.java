package com.textifyer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class LanguageAdapter extends BaseAdapter {

    private final Context context;
    private final List<LanguageItem> languages;

    public LanguageAdapter(Context context, List<LanguageItem> languages) {
        this.context = context;
        this.languages = languages;
    }

    @Override
    public int getCount() {
        return languages.size();
    }

    @Override
    public Object getItem(int position) {
        return languages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.language_spinner_item, parent, false);
        LanguageItem item = languages.get(position);

        ImageView flag = view.findViewById(R.id.flag_icon);
        TextView name = view.findViewById(R.id.language_name);

        flag.setImageResource(item.getFlagResId());
        name.setText(item.getLanguageName());

        return view;
    }
}

