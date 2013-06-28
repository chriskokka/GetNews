package controllers;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import models.Article;

public class  upArticle extends DefaultHandler {

    private Article article;
    private String temp;
    private ArrayList<Article> articleList = new ArrayList<>();
    private Boolean descript = false;
    private StringBuffer descSB = new StringBuffer();
    private int counter = 1;

    /*
        When the parser encounters characters between XML tags it comes here.
        If descipt is true , then it will append to StringBuffer. This is because
        of the differnet ways descriptions of articles are generated.
     */
    public void characters(char[] buffer, int start, int length) {
        temp = new String(buffer, start, length);
        if(descript){
            descSB.append(temp);
        }
    }

    /*
        When the parser encounters a start element it comes here.
     */
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes) throws SAXException {
        temp = "";

        if (qName.equalsIgnoreCase("doc")) {
            article = new Article();
        }
        if(qName.equalsIgnoreCase("description")){
            descript = true;
        }

    }

    /*
        When the parser encounter an end element it comes here.
        Then it checks what kind of element it was to set the correct
        field.(title, description, pubDate, source, or link)
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        try{
            if (qName.equalsIgnoreCase("doc")) {
                article.setNumber(counter++);
                articleList.add(article);
            } else if (qName.equalsIgnoreCase("title")) {
                article.setTitle(temp);
            } else if (qName.equalsIgnoreCase("description")) {
                article.setDescription(descSB.toString());
                descript = false;
                descSB = new StringBuffer();
            } else if (qName.equalsIgnoreCase("source")) {
                article.setSource(temp);
            }else if (qName.equalsIgnoreCase("link")) {
                article.setLink(temp);
            }else if (qName.equalsIgnoreCase("pubDate")) {
                article.setPubDate(temp);
            }
            }catch(NullPointerException e){
            }


    }

    /*
        Returns the list of articles parsed from the SOLR requests.
     */
    public ArrayList returnList() {
        return articleList;
    }

}