package ru.ifmo.md.lesson5;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;

public class RSSPager extends FragmentActivity {
    FeedMenuAdapter feedAdapter;
    ViewPager mViewPager;
    private static boolean online;
    ArrayList<String> defaultFeeds = new ArrayList<String>(Arrays.asList("http://stackoverflow.com/feeds/tag/android",
                                           "http://feeds.bbci.co.uk/news/rss.xml",
                                           "http://echo.msk.ru/interview/rss-fulltext.xml",
                                           "http://bash.im/rss/")), feeds = new ArrayList<String>();
    ArrayList<String> defaultFeedsNames = new ArrayList<String>(Arrays.asList("StackOverflow/Android",
            "BBC News",
            "Эхо Москвы",
            "Bash")), feedsNames = new ArrayList<String>();
    private int currPage;

    class NewFeedListener implements DialogInterface.OnClickListener {
        Handler mainThread;
        RSSPager p;
        EditText nameField, linkField;
        public NewFeedListener(Handler h, RSSPager pg, EditText n, EditText l) {
            mainThread = h;
            p = pg;
            nameField = n;
            linkField = l;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final String name = nameField.getText().toString();
            final String link = linkField.getText().toString();
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    p.addNewFeed(link, name);
                }
            });
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_demo);

        feedAdapter = new FeedMenuAdapter(getSupportFragmentManager(), this);
        dbGetFeeds();
        if (feeds.isEmpty()) {
            for (int i = 0; i < defaultFeeds.size(); i++) {
                dbWriteFeed(defaultFeeds.get(i), defaultFeedsNames.get(i));
            }
            dbGetFeeds();
        }
        for (int i = 0; i < feeds.size(); i++) {
            feedAdapter.add(feeds.get(i), feedsNames.get(i));
        }
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(feedAdapter);
        mViewPager.setOffscreenPageLimit(Integer.MAX_VALUE >> 1);
        currPage = 0;
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currPage = position;
            }

            public void onPageSelected(int position) {
                currPage = position;
            }
        });

        online = isOnline();
        if (!online) {
            Toast t = Toast.makeText(this, getString(R.string.notOnline), Toast.LENGTH_LONG);
            t.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void dbWriteFeed(String url, String title) {
        ContentValues values = new ContentValues();
        values.put(FeedsProvider.NAME, "#" + url);
        values.put(FeedsProvider.TITLE, title);
        values.put(FeedsProvider.DESC, "not null");
        values.put(FeedsProvider.LINK, "not null");
        getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
    }

    private void dbDeleteFeed(String url) {
        String args[] = {url, "#" + url};
        getContentResolver().delete(FeedsProvider.CONTENT_URI, "name=? OR name=?", args);
    }

    // write to feeds & feedsNames
    private void dbGetFeeds() {
        feeds.clear();
        feedsNames.clear();

        Cursor c = getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name LIKE '#%'", null, "name");
        while (c.moveToNext()) {
            String url = c.getString(c.getColumnIndex(FeedsProvider.NAME));
            url = url.substring(1, url.length());
            feeds.add(url);
            feedsNames.add(c.getString(c.getColumnIndex(FeedsProvider.TITLE)));
        }
        c.close();
    }

    private void addNewFeed(String link, String name) {
        if (name == null || link == null || !feedAdapter.add(link, name)) {
            Toast t = Toast.makeText(this, getString(R.string.noNewFeed), Toast.LENGTH_LONG);
            t.show();
        }
        else {
            dbWriteFeed(link, name);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_feed:
                feedAdapter.refresh(currPage);
                return true;
            case R.id.add_feed:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.newFeedTitle));
                final EditText nameField = new EditText(this);
                nameField.setText("Unnamed");
                final EditText linkField = new EditText(this);
                linkField.setText("http://");
                builder.setView(nameField);
                builder.setView(linkField);
                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.addView(nameField);
                ll.addView(linkField);
                builder.setView(ll);
                builder.setPositiveButton("OK", new NewFeedListener(new Handler(), this, nameField, linkField));
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();

                return true;
            case R.id.del_feed:
                dbDeleteFeed(feedAdapter.feeds.get(currPage));
                feedAdapter.del(currPage);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static class FeedMenuAdapter extends FragmentStatePagerAdapter {
        ArrayList<FeedMenu> fragments = new ArrayList<FeedMenu>();
        ArrayList<String> feeds = new ArrayList<String>();
        ArrayList<String> feedNames = new ArrayList<String>();
        Context mainContext;

        public FeedMenuAdapter(FragmentManager fm, Context c) {
            super(fm);
            mainContext = c;
        }

        // false if there's no new feed
        public boolean add(String url, String name) {
            if (feeds.indexOf(url) == -1) {
                feeds.add(url);
                feedNames.add(name);
                notifyDataSetChanged();
                return true;
            }
            return false;
        }

        public void refresh(int pos) {
            fragments.get(pos).refresh();
        }

        public void del(int pos) {
            feeds.remove(pos);
            feedNames.remove(pos);
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int i) {
            FeedMenu fragment = new FeedMenu(mainContext);
            Bundle args = new Bundle();
            args.putString(FeedMenu.ARG_URL, feeds.get(i));
            args.putBoolean(FeedMenu.ARG_ONLINE, online);
            fragment.setArguments(args);
            if (i == fragments.size()) {
                fragments.add(fragment);
            }
            else if (i < fragments.size()) {
                fragments.set(i, fragment);
            }
            else {
                Log.d("RSSPager", "Fragments mismatch");
            }
            return fragment;
        }

        @Override
        public int getItemPosition(Object object){
            // nasty viewpager bug: insertion after deleting not a last fragment isn't available
            // (new Fragment will be created, but not new View)
            // so let's redraw all view, no saving fragments
            return FragmentStatePagerAdapter.POSITION_NONE;
        }

        @Override
        public int getCount() {
            return feeds.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return feedNames.get(position);
        }
    }

    public static class FeedMenu extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        public static final String ARG_URL = "RSS_url", ARG_ONLINE = "RSS_online";
        String url;
        private boolean online;
        ListView lv;
        FeedCursorAdapter mAdapter;
        View rootView;
        Context mainContext;
        Cursor c;

        public FeedMenu(Context c) {
            mainContext = c;
        }

        public void refresh() {
            if (online) {
                getLoaderManager().initLoader(1, null, this).forceLoad();
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_collection_object, container, false);
            lv = (ListView) rootView.findViewById(R.id.list);
            Bundle args = getArguments();
            url = args.getString(ARG_URL);
            online = args.getBoolean(ARG_ONLINE);

            String[] arg = {url};
            c = mainContext.getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
            mAdapter = new FeedCursorAdapter(mainContext, R.layout.list_item, c, 0);
            lv.setAdapter(mAdapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(view.getContext(), WebActivity.class);
                    intent.putExtra(WebActivity.REQUEST_URL, (String)((TwoLineListItem) view).getText1().getTag());
                    intent.putExtra(WebActivity.REQUEST_TITLE, ((TwoLineListItem) view).getText1().getText().toString());
                    startActivity(intent);
                }
            });

            if (c.getCount() == 0)
                refresh();

            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            c.close();
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
            LinearLayout progressBarLayout = (LinearLayout) rootView.findViewById(R.id.linlaHeaderProgress);
            String[] arg = {url};
            c = mainContext.getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
            if (progressBarLayout != null && c.getCount() == 0)
                progressBarLayout.setVisibility(View.VISIBLE);
            return new FetchRSS(url, mainContext);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> voidLoader, Cursor c) {
            LinearLayout progressBarLayout = (LinearLayout) rootView.findViewById(R.id.linlaHeaderProgress);
            if (progressBarLayout != null)
                progressBarLayout.setVisibility(View.GONE);
            this.c = c;
            mAdapter = new FeedCursorAdapter(mainContext, R.layout.list_item, this.c, 0);
            lv.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> voidLoader) {}
    }
}