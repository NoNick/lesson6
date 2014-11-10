package ru.ifmo.md.lesson5;

import android.support.v4.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

class FetchRSS extends AsyncTaskLoader<Cursor> {
    String url;
    Context context;
    private final String RSSTag = "item", AtomTag = "entry";

    FetchRSS(String u, Context c) {
        super(c);
        url = u;
        context = c;
    }

    @Override
    public Cursor loadInBackground() {
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
            context.getContentResolver().delete(FeedsProvider.CONTENT_URI, "name=?", arg);
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
                        context.getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
                    }
                    else {
                        ContentValues values = new ContentValues();
                        values.put(FeedsProvider.NAME, url);
                        values.put(FeedsProvider.TITLE, e.getElementsByTagName("title").item(0).getFirstChild().getNodeValue());
                        values.put(FeedsProvider.DESC, e.getElementsByTagName("description").item(0).getFirstChild().getNodeValue());
                        values.put(FeedsProvider.LINK, e.getElementsByTagName("link").item(0).getFirstChild().getNodeValue());
                        context.getContentResolver().insert(FeedsProvider.CONTENT_URI, values);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] arg = {url};
        return context.getContentResolver().query(FeedsProvider.CONTENT_URI, null, "name=?", arg, null);
    }
}
