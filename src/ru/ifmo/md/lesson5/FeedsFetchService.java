package ru.ifmo.md.lesson5;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class FeedsFetchService extends IntentService implements LoaderManager.LoaderCallbacks<Cursor>{
    private String url;
    private boolean force;
    private final String RSSTag = "item", AtomTag = "entry";
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
        intent.getBooleanExtra("force", force);

        try {
            URL url_ = new URL(url);
            Document xmlResponse = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    (InputStream) url_.getContent());
            xmlResponse.getDocumentElement().normalize();
            NodeList list = xmlResponse.getElementsByTagName(RSSTag);
            if (url.contains("stackoverflow")) {
                list = xmlResponse.getElementsByTagName(AtomTag);
            }

            String[] arg = {url};
            Cursor c = getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
            if (c.getCount() == 0 || force) {
                Log.d("RssFetchService", "Fetch data for feed " + url);
                getContentResolver().delete(FeedsProvider.CONTENT_URI, "name=?", arg);
                for (int i = 0; i < list.getLength(); i++) {
                    Node node = list.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) node;
                        if (url.contains("stackoverflow")) {
                            ContentValues values = new ContentValues();
                            values.put(FeedsProvider.NAME, url);
                            values.put(FeedsProvider.TITLE, e.getElementsByTagName("title").item(0).getFirstChild().getNodeValue());
                            values.put(FeedsProvider.DESC, e.getElementsByTagName("name").item(0).getFirstChild().getNodeValue());
                            values.put(FeedsProvider.LINK, e.getElementsByTagName("id").item(0).getFirstChild().getNodeValue());
                            getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
                        } else {
                            ContentValues values = new ContentValues();
                            values.put(FeedsProvider.NAME, url);
                            values.put(FeedsProvider.TITLE, e.getElementsByTagName("title").item(0).getFirstChild().getNodeValue());
                            values.put(FeedsProvider.DESC, e.getElementsByTagName("description").item(0).getFirstChild().getNodeValue());
                            values.put(FeedsProvider.LINK, e.getElementsByTagName("link").item(0).getFirstChild().getNodeValue());
                            getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
                        }
                    }
                }
            }
            else {
                Log.d("RssFetchService", "Use old data for feed " + url);
            }
            c.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class AtomRSSParser extends DefaultHandler {
        private boolean saveText;
        private String currentText = "";
        private ContentValues currentValues;

        @Override
        public void startElement(String string, String localName, String qName, Attributes attrs) throws SAXException {
            if (qName.equals("item") || qName.equals("entry")) {
                currentValues = new ContentValues();
            } else if (qName.equals("title") || qName.equals("link")) {
                saveText = true;
                currentText = "";
            }
            if (currentValues != null && qName.equals("link")) {
                currentValues.put("url", attrs.getValue("href"));
            }
        }

        @Override
        public void endElement(String string, String localName, String qName) throws SAXException {
            saveText = false;
            if (currentValues != null) {
                if (localName.equals("title")) {
                    currentValues.put("title", currentText.trim());
                } else if (localName.equals("link") && !currentText.isEmpty()) {
                    currentValues.put("url", currentText);
                } else if (localName.equals("item") || localName.equals("entry")) {
//                    getContentResolver().insert(postsUri, currentValues);
                    currentValues = null;
                }
            } else if (localName.equals("title")) {
                String newTitle = currentText.trim();
/*                if (!newTitle.equals(feedTitle)) {
                    ContentValues values = new ContentValues();
                    values.put("title", newTitle);
                    getContentResolver().update(feedUri, values, null, null);
                }*/
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String strCharacters = new String(ch, start, length);
            if (saveText) currentText += strCharacters;
        }
    }
}