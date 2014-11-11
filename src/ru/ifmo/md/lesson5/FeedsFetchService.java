package ru.ifmo.md.lesson5;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class FeedsFetchService extends IntentService implements LoaderManager.LoaderCallbacks<Cursor>{
    private String url;
    private boolean force;
    Cursor c;
    public FeedsFetchService() {
        super("FeedsFetchService");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] arg = {url};
        c = getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> voidLoader, Cursor c) {
        this.c = c;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        url = intent.getStringExtra("url");
        force = intent.getExtras().getBoolean("force");

        try {
            String[] arg = {url};
            Cursor c = getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
            if (c.getCount() == 0 || force) {
                Log.d("RssFetchService", "Fetch data for feed " + url);

                getContentResolver().delete(FeedsProvider.CONTENT_URI, "name=?", arg);
                SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
                SAXParser saxParser = saxParserFactory.newSAXParser();
                XMLReader xmlReader = saxParser.getXMLReader();
                AtomRSSParser rssHandler = new AtomRSSParser(url);
                xmlReader.setContentHandler(rssHandler);
                InputSource inputSource = new InputSource(new URL(url).openStream());
                xmlReader.parse(inputSource);
            }
            else {
                Log.d("RssFetchService", "Use old data for feed " + url);
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class AtomRSSParser extends DefaultHandler {
        private String text, url;
        private ContentValues values;
        // display summary.substring(0, charN)
        private final int charN = 500;

        public AtomRSSParser(String url) {
            this.url = url;
        }

        @Override
        public void startElement(String string, String localName, String qName, Attributes attrs) throws SAXException {
            if (qName.equals("item") || qName.equals("entry")) {
                values = new ContentValues();
                values.put(FeedsProvider.NAME, url);
            }
            else if (qName.equals("title") || qName.equals("description") || qName.equals("summary")) {
                text = "";
            }
            if (values != null && qName.equals("link")) {
                values.put(FeedsProvider.LINK, attrs.getValue("href"));
            }
        }

        @Override
        public void endElement(String string, String localName, String qName) throws SAXException {
            if (values != null) {
                if (localName.equals("title")) {
                    values.put(FeedsProvider.TITLE, text.trim());
                }
                else if (qName.equals("description")) {
                    values.put(FeedsProvider.DESC, text.trim());
                }
                else if (qName.equals("summary")) {
                    values.put(FeedsProvider.DESC, text.trim().substring(0, Math.min(charN, text.trim().length())));
                }
                else if (localName.equals("link") && !text.isEmpty()) {
                    values.put(FeedsProvider.LINK, text);
                }
                else if (localName.equals("item") || localName.equals("entry")) {
                    getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
                    values = null;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            text += new String(ch, start, length);
        }
    }
}