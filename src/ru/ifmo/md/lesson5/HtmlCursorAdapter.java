package ru.ifmo.md.lesson5;

import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class HtmlCursorAdapter extends SimpleCursorAdapter {

    public HtmlCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    @Override
    public void setViewText (TextView view, String text) {
        view.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
    }
}