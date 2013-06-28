package models;


public class Feed {

    public String url;
    public String source;

    public Feed(){
    }

    public Feed(String url, String source){
        this.url = url;
        this.source = source;
    }

    public String getUrl(){
        return url;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getFeedSource(){
        return source;
    }

    public void setFeedSource(String source){
        this.source = source;
    }
}
