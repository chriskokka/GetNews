package controllers;

import play.mvc.*;
import play.data.Form;

import models.User;
import models.Article;
import models.Feed;
import models.MoreFeed;

import java.net.HttpURLConnection;

import java.io.*;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.FeedException;



public class Application extends Controller {

    /*
        signupForm - the empty form for the signup page.
        feedForm - the empty form for the new feed adding page.
        moreFeedForm - the empty form for the user page to specify which feed the user wants more posts for.
        loggedUser - static variable of what user is logged.
    */

    public static Form<User> signupForm = Form.form(User.class);
    public static Form<Feed> feedForm = Form.form(Feed.class);
    public static Form<MoreFeed> moreFeedForm = Form.form(MoreFeed.class);
    public static String loggedUser = "Guest";


    /*
        Function displays the front page.
     */
    public static Result index() {
      return ok(views.html.index.render(loggedUser));
    }




    /*
        Function will simply display the page where a user can
        add a new feed to subscribe to.
     */
    public static Result showFeedPage(){
        return ok(views.html.newFeed.render(feedForm,loggedUser));
    }


    /*
        Function will add a new feed to the users feed list.
        2 paramaters are gotten from the Form:
            source - the name of the feed. In the form of: news.com or goodnews.net and etc
            url - the url of the feed.
     */
    public static Result addFeed(){
        Form<Feed> filledForm = feedForm.bindFromRequest();
        User foundUser = User.getUser(loggedUser);
        foundUser.sources = foundUser.sources + "+" + filledForm.get().source;
        foundUser.urls = foundUser.urls +  "+" + filledForm.get().url;
        foundUser.save();
        getContent(filledForm.get().url,filledForm.get().source);
        updateSolrOneFile(filledForm.get().source);
        try{
            Thread.sleep(1 * 2500);
        }catch(InterruptedException e){
            e.printStackTrace();
        }


        return redirect(routes.Application.update());
    }

    /*
        Function will send a request to solr to get posts from 1-10
        for each feed the user has subscribed to.
     */
    public static Result update(){

        if(loggedUser.equals("Guest")){
            return redirect(routes.Application.login());
        }else{

            User foundUser = User.getUser(loggedUser);
            String sources = foundUser.sources;
            System.out.println("The sources going to request are: "+sources);
            String[] sourceList = sources.split("\\+");


            try{
                File file = new File("/GetNews/assets/solrreq.xml");
                if(file.exists()) {
                    file.delete();
                    file.createNewFile();
                }else{
                    file.createNewFile();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/GetNews/assets/solrreq.xml", true)));
                out.println("<root>");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for(String source: sourceList){
                if(source.equals("")){
                    continue;
                }
                getNewsFromSolr("solrreq",source.replaceFirst("\\.(.*)",""),0,10);
            }

            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/GetNews/assets/solrreq.xml", true)));
                out.println("</root>");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            upArticle handler   = new upArticle();
            saxParser.parse("/GetNews/assets/solrreq.xml", handler);
            ArrayList<Article> articleList = handler.returnList();

            return ok(views.html.userpage.render(articleList,loggedUser,moreFeedForm));

        } catch (Throwable err) {
            err.printStackTrace ();
        }

        return redirect(routes.Application.index());
    }

    /*
        Function will check what source to send the request to solr about.
        Data will be save to file: moreSolrReq.xml
        The request will be to display all the posts from 11th to infinity.
        Or 100 right now.
     */
    public static Result moreNews(){
        if(loggedUser.equals("Guest")){
            return redirect(routes.Application.login());
        }else{
            Form<MoreFeed> filledForm = moreFeedForm.bindFromRequest();
            getNewsFromSolr("moreSolrReq",filledForm.get().moreNews,10,100);
        }
        return redirect(routes.Application.showMoreNews());
    }

    /*
        Function will parse the data from moreSolrReq.xml that contains
        the posts 11 to 100 from the source that was specified in function:
        moreNews()
     */
    public static Result showMoreNews(){
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            upArticle handler   = new upArticle();
            saxParser.parse("/GetNews/assets/moreSolrReq.xml", handler);
            ArrayList<Article> articleList = handler.returnList();
            return ok(views.html.moreNews.render(articleList,loggedUser));

        } catch (Throwable err) {
            err.printStackTrace ();
        }
        return redirect(routes.Application.index());
    }


    /*
        Function simply displays the signup page with no user info.
     */
    public static Result signup(){
        return ok(views.html.signup.render(signupForm));
    }

    /*
        Function verifies all the user info fields and if everything
        passes it saves the user to the database.
     */
    public static Result addUser(){
        Form<User> filledForm = signupForm.bindFromRequest();
        String enteredUsername = filledForm.get().username;
        String enteredPassword = filledForm.get().password;
        String enteredRepeatPassword = filledForm.field("repeatPassword").value();


        if(enteredPassword.contentEquals("") || enteredRepeatPassword.contentEquals("")){
            return badRequest(views.html.signup.render(filledForm));
        }

        if(!(enteredPassword.contentEquals(enteredRepeatPassword))){
            filledForm.reject("repeatPassword","Passwords did not match!");
        }


        boolean uniqueUser = User.uniqueUser(enteredUsername);
        if(!uniqueUser){
            filledForm.reject("username", "Sorry, that username is already taken!");
        }

        if(filledForm.hasErrors()) {
            return badRequest(views.html.signup.render(filledForm));
        }
        User.create(filledForm.get());
        return redirect(routes.Application.login());
    }


    /*
        Function verifies if the entered password is correct and whether
        the password or username field was left empty.

        DUE TO LACK OF TIME NO SPECIAL USER VERIFICATION IS DONE.

        NOTE: FIX THIS IN THE FUTURE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

     */
    public static Result login(){
        Form<User> filledForm = signupForm.bindFromRequest();
        String formUsername = filledForm.get().username;
        String formPassword = filledForm.get().password;

        if(formUsername.contentEquals("") || formPassword.contentEquals("")){
            return badRequest(views.html.login.render(filledForm));
        }
        User user = User.authenticate(formUsername,formPassword);
        if(user != null){
            loggedUser = formUsername;
            getFeedSources();
            return redirect(routes.Application.index());
        }else{
            filledForm.reject("password", "Incorrect password or username");
        }
        return badRequest(views.html.login.render(filledForm));

    }

    /*
        Function simply sets the current user to: Guest and
        displays the empty login page.
     */
    public static Result goToLogin(){
        loggedUser = "Guest";
        return ok(views.html.login.render(signupForm));
    }

    /*
        Function will check what feeds the user has signed up for and
        display them on the settings page.
     */
    public static Result settings(){
        if(loggedUser.equals("Guest")){
            return redirect(routes.Application.login());
        }
        User foundUser = User.getUser(loggedUser);
        String sources = foundUser.sources;
        String urls    = foundUser.urls;
        String[] sourceList = {};

        try{
            sourceList = sources.split("\\+");
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        List<String> listOfFeeds = new ArrayList<>();
        for(int i=0;i<sourceList.length;i++){
            if(sourceList[i].contains("null")){
                continue;
            }else{
                try{
                    listOfFeeds.add(i-1,sourceList[i]);
                }catch(IndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }
        }


        return ok(views.html.settings.render(signupForm,loggedUser,listOfFeeds));
    }

    /*
        Function will save the settings that the user has made on the settings page.
     */
    public static Result saveSettings(){
        Form<User> filledForm = signupForm.bindFromRequest();
        User foundUser = User.getUser(loggedUser);
		
        String[] listOfUserUrls = foundUser.urls.split("\\+");
        String temp = filledForm.get().sources;
        String[] list = temp.split("\\+");
		
        for(int i=0;i<list.length;i++){
            if(list[i].equals("")){
                list[i] = "11143431t4t431h1hh1116u56u11rt11trt111d11sfasf1111";
            }
        }

        String newSources = "";
        String newUrls    = "";
        for(int x=0;x<listOfUserUrls.length;x++){
            for(int y=0;y<list.length;y++){
                if(listOfUserUrls[x].contains(list[y])){
                    newSources = newSources + '+' + list[y];
                    newUrls = newUrls + '+' + listOfUserUrls[x];
                }
            }
        }

        foundUser.sources = newSources;
        foundUser.urls = newUrls;
        foundUser.save();

        return redirect(routes.Application.index());

    }

    /*
        Function will send the request to solr based on the query parameters and will put it
        into a format that the XML parser can parse. SAXParser is used for that at the moment.
        Params:
            file - the name of the file that the solr request data is going. It can be eiter
            solrreq(for the first 10 posts to the userpage) or moreSolrReq(all the posts from 11th to infinity).
            source - the name of the feed that  is going to be parsed and given out.
            start - the row where to start giving out items.
            rows - the number of rows asked in solr request.
     */
    public static void getNewsFromSolr(String file,String source,int start ,int rows){
        try {
            URL my_url = new URL("http://localhost:8983/solr/select?q=source:"+source+"*&start="+start+"&rows="+rows+"&wt=xml&indent=true");
            BufferedReader br = new BufferedReader(new InputStreamReader(my_url.openStream()));
            String strTemp = "";
            String contentOut = "";


            if(file.contains("moreSolrReq")){
                try{
                    File fileForReq = new File("/GetNews/assets/moreSolrReq.xml");
                    if(fileForReq.exists()) {
                        fileForReq.delete();
                        fileForReq.createNewFile();
                    }else{
                        fileForReq.createNewFile();
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            if(file.contains("moreSolrReq")){
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/GetNews/assets/moreSolrReq.xml", true)));
                    out.println("<root>");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

			// Modifying the Solr response for the SAXParser.
			
            while(null != (strTemp = br.readLine())){
                String content = "";
                String content2 = "";

                if(strTemp.contains("name=\"title\"")){
                    content = strTemp.replaceFirst("<str name=\"title\">","<title>");
                    content2 = content.replaceAll("/str","/title");
                }
                if(strTemp.contains("name=\"pubDate\"")){
                    content = strTemp.replaceFirst("str name=\"pubDate\"","pubDate");
                    content2 = content.replaceAll("/str","/pubDate");
                }
                if(strTemp.contains("name=\"description\"")){
                    content = strTemp.replaceFirst("str name=\"description\"","description");
                    content2 = content.replaceAll("/str","/description");
                }
                if(strTemp.contains("name=\"source\"")){
                    content = strTemp.replaceFirst("str name=\"source\"","source");
                    content2 = content.replaceAll("/str","/source");
                }
                if(strTemp.contains("name=\"link\"")){
                    content = strTemp.replaceFirst("<str name=\"link\">","<link>");
                    content2 = content.replaceAll("</str>","</link>");
                }

                if(content2.equals("")){
                    contentOut = strTemp;
                }else{
                    contentOut = content2;
                }


                if(contentOut.contains("</str>")){
                    contentOut = contentOut.replaceAll("</str>","</description>");
                }



                if(contentOut.equals("<response>")){
                    contentOut = "";
                }else if(contentOut.equals("</response>")){
                    contentOut = "";
                } else if(contentOut.equals("</result>")){
                    contentOut = "";
                }else if(contentOut.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")){
                    contentOut = "";
                }else if(contentOut.contains("<lst name=\"responseHeader\">")){
                    contentOut = "";
                }else if(contentOut.contains("<int name=\"status\">")){
                    contentOut = "";
                }else if(contentOut.contains("<int name=\"QTime\">")){
                    contentOut = "";
                }else if(contentOut.contains("<lst name=\"params\">")){
                    contentOut = "";
                }else if(contentOut.contains("<str name=\"indent\">")){
                    contentOut = "";
                }else if(contentOut.contains("<str name=\"q\">source:")){
                    contentOut = "";
                }else if(contentOut.contains("<str name=\"wt\">")){
                    contentOut = "";
                }else if(contentOut.contains("<str name=\"rows\">")){
                    contentOut = "";
                }else if(contentOut.contains("</lst>")){
                    contentOut = "";
                }else if(contentOut.contains("<result name=")){
                    contentOut = "";
                }else if(contentOut.contains("<str name=\"start\"")){
                    contentOut = "";
                }



                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/GetNews/assets/"+file+".xml", true)));
                    out.println(contentOut);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(file.contains("moreSolrReq")){
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/GetNews/assets/moreSolrReq.xml", true)));
                    out.println("</root>");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /*
        Function gets the source of the feed and puts it into a specific xml form
        for Solr.

        Params:
            url: The url to get the feed content from.
            source: The identifier used for solr request. In the form of: somewebsite.com or news.net
     */
    public static void getContent(String url,String source){
        try{
            URL my_url = new URL(url);
            HttpURLConnection httpcon = (HttpURLConnection)my_url.openConnection();

            // Reading the feed
            try{
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(httpcon));
                List entries = feed.getEntries();
                Iterator itEntries = entries.iterator();
                String contentOut;

                /*
                    solrFile specifies the the file to save data from solr request. It can be either
                    be: solrreq(for the first 10 posts to the userpage) or moreSolrReq(all the posts from 11th to infinity).
					"solr-4.3.0\\GetNews\\docs\\"+solrFile+".xml" is the path to the solr dir where the data
					files that are going to be uploaded are stored.
                 */

                String solrFile = source.replaceFirst("\\.(.*)","");
                try{
                    File file = new File("solr-4.3.0\\GetNews\\docs\\"+solrFile+".xml"); 
                    if(file.exists()){
                        file.delete();
                        file.createNewFile();
                    }else{
                        file.createNewFile();
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }

                try {

                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("solr-4.3.0\\GetNews\\docs\\"+solrFile+".xml", true)));
                    out.println("<add>");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (itEntries.hasNext()) {
                    SyndEntry entry = (SyndEntry)itEntries.next();
                    try {

                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("solr-4.3.0\\GetNews\\docs\\"+solrFile+".xml", true)));
                        out.println("\t<doc>\n");
                        out.println("\t\t<field name=\"title\">" + entry.getTitle().toString().replaceAll("&","") + "</field>");
                        out.println("\t\t<field name=\"pubDate\">" + entry.getPublishedDate() + "</field>");

                        contentOut = entry.getDescription().getValue().replaceAll("&nbsp;","");
                        contentOut = contentOut.replaceAll("&","");
                        contentOut = contentOut.replaceAll("<p><div class=\"share_submission\"(.*)","");

                        if(entry.getDescription().toString().contains("<img src=")){
                            contentOut = contentOut.replaceAll("<img src=(.*)","");
                        }
                        out.println("\t\t<field name=\"description\">" + contentOut + "</field>");

                        out.println("\t\t<field name=\"source\">" + source + "</field>");
                        out.println("\t\t<field name=\"link\">" + entry.getLink().toString().replaceAll("&ref=rss","") + "</field>");
                        out.println("\t</doc>");
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {

                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("solr-4.3.0\\GetNews\\docs\\"+solrFile+".xml", true)));
                    out.println("</add>");
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }catch(FeedException e){
                e.printStackTrace();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /*
        Function updates all the files in the Solr file directory.
     */
    public static void updateSolr(){
        try{
            String[] command = new String[]{"java", "-jar",  "post.jar", "*.xml"};
            Runtime.getRuntime().exec(command,null, new File("solr-4.3.0\\GetNews\\docs\\"));
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    /*
        Function updates a single file in the Solr file directory.
        Params:
            source: the name of the file you wish to update without the prefix.(eg. somenews  NOT somenews.xml)
     */
    public static void updateSolrOneFile(String source){
        try{
            String[] command = new String[]{"java", "-jar",  "post.jar", source.replaceAll("\\.(.*)","")+".xml"};
            Runtime.getRuntime().exec(command,null, new File("solr-4.3.0\\GetNews\\docs\\"));
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    /*
        Function goes through all the feeds that are saved for this user and then
        updates all the Solr files.
     */
    public static void getFeedSources(){
            User foundUser = User.getUser(loggedUser);
            try{
                String[] sourceList = foundUser.sources.split("\\+");
                String[] urlList = foundUser.urls.split("\\+");
                for(int i=0;i<sourceList.length;i++){
                    if(sourceList[i].equals("null")){
                        continue;
                    }else{
                        getContent(urlList[i],sourceList[i]);
                    }
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            updateSolr();
    }
}


