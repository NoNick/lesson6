package ru.ifmo.md.lesson5;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

public class FeedCursorAdapter extends ResourceCursorAdapter {

    Context mContext;
    int layoutResourceId;

    public FeedCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);

        layoutResourceId = layout;
        mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView text1 = ((TwoLineListItem) view).getText1();
        TextView text2 = ((TwoLineListItem) view).getText2();
        text1.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(FeedsProvider.TITLE))));
        text1.setTag(cursor.getString(cursor.getColumnIndex(FeedsProvider.LINK)));
        text2.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndex(FeedsProvider.DESC))));
    }

}
