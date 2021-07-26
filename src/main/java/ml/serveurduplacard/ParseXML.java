package ml.serveurduplacard;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hello world!
 *
 */
public class ParseXML {

    @Parameter(names = "-i", description = "Xml input file")
    String xmlInput;

    @Parameter(names = "-h", description = "Database URL, default; localhost:3306")
    String dbHost = "localhost:3306";

    @Parameter(names = "-u", description = "Database user name, default: root")
    String dbUser = "root";

    @Parameter(names = "-p", description = "Database password, default: \"\"")
    String dbPassword = "";

    @Parameter(names = "-s", description = "Batch insertions size, default: 100")
    int batchSize = 100;




    public static void main( String[] args ) throws ParserConfigurationException, IOException, SAXException {



        ParseXML arguments = new ParseXML();

        JCommander jc = JCommander.newBuilder()
                .addObject(arguments)
                .build();

        jc.parse(args);

        if(arguments.xmlInput == null) {
            jc.usage();
            System.exit(-1);
        }


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(new File(arguments.xmlInput)));



        NodeList articles = doc.getElementsByTagName("PubmedArticle");


        int expected = articles.getLength();
        int successes = 0;

        for(int i = 0; i < articles.getLength(); i++) {
            Node article = articles.item(i);
            try {

                String title = null;
                String lang = null;
                String mabstract = null;
                String journal = null;
                try {
                    title = ((Element) article).getElementsByTagName("ArticleTitle").item(0).getTextContent();
                    lang = ((Element) article).getElementsByTagName("Language").item(0).getTextContent();
                    mabstract = ((Element) article).getElementsByTagName("Abstract").item(0).getTextContent();
                    journal = ((Element) ((Element) article).getElementsByTagName("Journal").item(0)).getElementsByTagName("Title").item(0).getTextContent();
                } catch (NullPointerException e) {}


                if(title == null) {
                    logFailure("error: no title");
                    continue;
                }
                if(lang == null) {
                    logFailure("error: no lang");
                    continue;
                }
                if(mabstract == null) {
                    logFailure("error: no abstract");
                    continue;
                }
                if(journal == null) {
                    logFailure("error: no journal");
                    continue;
                }

                NodeList ids = ((Element) ((Element) article).getElementsByTagName("ArticleIdList").item(0)).getElementsByTagName("ArticleId");

                String doi = null;
                String spotedIds = "";
                for (int j = 0; j < ids.getLength(); j++) {
                    Element id = (Element) ids.item(j);
                    spotedIds += "," + id.getAttribute("IdType");
                    if ("doi".equals(id.getAttribute("IdType"))) {
                        doi = id.getTextContent();
                        break;
                    }
                }


                String date = null;
                Element dateNode = ((Element) ((Element) article).getElementsByTagName("ArticleDate").item(0));

                String day = null, month = null, year = null;

                try {
                    day = dateNode.getElementsByTagName("Day").item(0).getTextContent();
                    month = dateNode.getElementsByTagName("Month").item(0).getTextContent();
                    year = dateNode.getElementsByTagName("Year").item(0).getTextContent();

                    date = year + "/" + month + "/" + day;
                } catch (NullPointerException e) {}

                if(date == null) {
                    logFailure("error: no date");
                    continue;
                }


                Map<String,String> authors = new LinkedHashMap<>();


//<AuthorList CompleteYN="Y">
//<Author ValidYN="Y">
//<LastName></LastName> = authors.last_name
//<ForeName></ForeName> = authors.first_name
//</Author>
//</AuthorList>
                boolean failure = false;
                NodeList authorList = ((Element) article).getElementsByTagName("AuthorList");
                for(int j = 0; j < authorList.getLength(); j++) {
                    try {
                        String fName = null;
                        String lName = null;

                        lName = ((Element) authorList.item(j)).getElementsByTagName("LastName").item(0).getTextContent();
                        fName = ((Element) authorList.item(j)).getElementsByTagName("ForeName").item(0).getTextContent();

                        authors.put(fName,lName);
                    } catch (NullPointerException e) {
                        logFailure("error: cannot read author ");
                        failure = true;
                        break;
                    }
                }
                if(failure) continue;


                String authorsStr = String.join(",", authors.entrySet().stream().map((e) -> e.getKey() + " " + e.getValue()).collect(Collectors.<String>toList()));



                if(doi == null) {
                    logFailure("error: " + spotedIds);
                } else {
                    System.out.println("authors: " + authorsStr + ", date: " + date + ", journal: " + journal + ", doi: " + doi + ", title: " + title + ", lang: " + lang);

                }
                successes++;
            } catch (Exception e) {
                logFailure("error: " + e.getClass() + ", " + e.getMessage());
            }

        }

        System.out.println("     -----------------------------------------------------");
        System.out.println("Successes: " + successes + " / " + expected);
    }


    public static void logFailure(String info) {
        System.err.println("error: " + info);
    }

}
//<AuthorList CompleteYN="Y">
//<Author ValidYN="Y">
//<LastName></LastName> = authors.last_name
//<ForeName></ForeName> = authors.first_name
//</Author>
//</AuthorList>
//
//
//<ArticleTitle></ArticleTitle> = articles.title
//
//<Language></Language> = articles.lang
//
//<Abstract></Abstract> = articles.abstract
//
//<ArticleIdList>
//<ArticleId IdType="doi"></ArticleId> = articles.doi
//</ArticleIdList>
//
//
//<ArticleDate DateType="Electronic">
//<Year></Year>
//<Month></Month> = articles.date
//<Day></Day>
//</ArticleDate>
//
//
//<Journal>
//<Title></Title>= venues.name
//</Journal>
//
//<PublicationTypeList>
//<PublicationType UI="D016428"></PublicationType> = venues.type
//</PublicationTypeList>