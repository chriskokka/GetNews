package models;



public class Article{
    private String title;
    private String description;
    private String source;
    private String link;
    private String pubDate;
    private int    number;

    public Article() {
    }

    public Article(String title, String description, String source, String link, String pubDate,int number) {
        this.title = title;
        this.description = description;
        this.source = source;
        this.link = link;
        this.pubDate = pubDate;
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLink(){
        return link;
    }

    public void setLink(String link){
        this.link = link;
    }

    public String getPubDate(){
        return pubDate;
    }

    public void setPubDate(String pubDate){
        this.pubDate = pubDate;
    }

    public int getNumber(){
        return number;
    }

    public void setNumber(int number){
        this.number = number;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\t<doc>");
        sb.append("\n");
        sb.append("\t\t<field name=\"title\">" + getTitle() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"pubDate\">" + getPubDate() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"description\">" + getDescription() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"source\">" + getSource() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"link\">" + getLink() + "</field>");
        sb.append("\n");
        sb.append("\t</doc>");


        return sb.toString();
    }

    public String toStringDZone() {
        StringBuffer sb = new StringBuffer();
        sb.append("\t<doc>");
        sb.append("\n");
        sb.append("\t\t<field name=\"title\">" + getTitle() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"pubDate\">" + getPubDate() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"description\">" + getDescription() + "</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"source\">java.dzone.com</field>");
        sb.append("\n");
        sb.append("\t\t<field name=\"link\">" + getLink() + "</field>");
        sb.append("\n");
        sb.append("\t</doc>");


        return sb.toString();
    }

}


