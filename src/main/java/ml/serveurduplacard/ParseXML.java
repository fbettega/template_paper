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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hello world!
 *
 */
public class ParseXML {

    @Parameter(names = "-i", description = "Xml input file or directory containing multiple inputs")
    String xmlInput;

    @Parameter(names = "-h", description = "Database URL, default; localhost:3306")
    String dbHost = "localhost:3306";

    @Parameter(names = "-n", description = "Database name, default: temparticle")
    String dbName = "temparticle";

    @Parameter(names = "-u", description = "Database user name, default: root")
    String dbUser = "root";

    @Parameter(names = "-p", description = "Database password, default: \"\"")
    String dbPassword = "";

    @Parameter(names = "-s", description = "Batch insertions size, default: 100")
    int batchSize = 100;


    public static void main( String[] args ) throws ParserConfigurationException, IOException, SAXException, SQLException {



        ParseXML arguments = new ParseXML();

        JCommander jc = JCommander.newBuilder()
                .addObject(arguments)
                .build();

        jc.parse(args);

        if(arguments.xmlInput == null) {
            jc.usage();
            System.exit(-1);
        }

        // ----------- Prepare DB connection ------------------------

        Connection db = DriverManager.getConnection("jdbc:mariadb://" +
                arguments.dbHost + "/" +
                arguments.dbName + "?user=" +
                arguments.dbUser + "&password=" +
                arguments.dbPassword
        );


        // ----------- Parse XML ------------------------------------

        File input = new File(arguments.xmlInput);

        List<File> inputFiles = new ArrayList<>();
        if(input.isDirectory()) {
            inputFiles.addAll(
                    Files.find(Paths.get(input.getAbsolutePath()),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".xml"))
                            .map(path -> new File(path.toString()))
                    .collect(Collectors.toList())
            );
        } else {
            inputFiles.add(input);
        }

        for (File f: inputFiles) {
            System.out.println("     -----------------------------------------------------");
            System.out.println("Start parsing file: " + f.getAbsolutePath());


            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new FileInputStream(f));



            NodeList articles = doc.getElementsByTagName("PubmedArticle");


            expected = articles.getLength();
            successes = 0;

            List<Article> buffer = new ArrayList<>();

            for(int i = 0; i < articles.getLength(); i++) {
                Node article = articles.item(i);
                try {
                    Article a = parse(article);
                    buffer.add(a);
                    if(buffer.size() >= arguments.batchSize) {
                        pushBatch(buffer, db);
                        buffer.clear();
                    }
                } catch (ArticleParseException e) {
                    logFailure(e.getMessage(), arguments.xmlInput);
                }

            }

            if(!buffer.isEmpty()) pushBatch(buffer, db);

            System.out.println("     -----------------------------------------------------");
            System.out.println("Successes: " + successes + " / " + expected);
            System.out.println("     -----------------------------------------------------");
        }
    }


    public static int successes = 0;

    public static int expected = 0;


    public static void logFailure(String info, String source) {
        System.err.println("error: " + info + " in file " + source);
    }

    public static Article parse(Node article) throws ArticleParseException {
        try {
            String pumedId = null;
            try {
                pumedId = ((Element) article).getElementsByTagName("PMID").item(0).getTextContent();
            } catch (NullPointerException e) {}

            if(pumedId == null) {
                throw new ArticleParseException("no pumedId");
            }



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
                throw new ArticleParseException("no title : " + pumedId);
            }
            if(lang == null) {
                throw new ArticleParseException("no lang : " + pumedId);
            }
            if(mabstract == null) {
                throw new ArticleParseException("no abstract : " + pumedId);
            }
            if(journal == null) {
                throw new ArticleParseException("no journal : " + pumedId);
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



            String date2 = null;
            Element dateNode2 = ((Element) ((Element) article).getElementsByTagName("PubDate").item(0));

            String day2 = null, month2 = null, year2 = null;

            try {
                NodeList yList = dateNode2.getElementsByTagName("Year");
                year2 = yList.getLength() > 0 ? yList.item(0).getTextContent() : null;

                date2 = year2;// + "/" + month2 + "/" + day2;
            } catch (NullPointerException e) {}

            if(date2 == null) {
                NodeList mList = dateNode2.getElementsByTagName("MedlineDate");
                if(mList != null && mList.getLength() > 0) {
                    date2 = mList.item(0).getTextContent().split(" ")[0];
                }
            }

            if(date2 == null) {
                Element dateNode3 = ((Element) ((Element) article).getElementsByTagName("ArticleDate").item(0));
                if(dateNode3 != null) {
                    NodeList yList = dateNode3.getElementsByTagName("Year");
                    date2 = yList.getLength() > 0 ? yList.item(0).getTextContent() : null;
                }


                if(date2 == null) {
                    throw new ArticleParseException("no date : " + pumedId);
                }
            }


            Map<String,String> authors = new LinkedHashMap<>();
            boolean failure = false;
            Node authorListNode = ((Element) article).getElementsByTagName("AuthorList").item(0);
            if(authorListNode == null) {
                throw new ArticleParseException("no author (no authorList) : " + pumedId);
            }
            NodeList authorList = ((Element) authorListNode).getElementsByTagName("Author");
            for(int j = 0; j < authorList.getLength(); j++) {
                try {
                    String fName = null;
                    String lName = null;

                    if(((Element) authorList.item(j)).getElementsByTagName("CollectiveName").getLength() > 0) continue;

                    lName = ((Element) authorList.item(j)).getElementsByTagName("LastName").item(0).getTextContent();

                    if(((Element) authorList.item(j)).getElementsByTagName("ForeName").getLength() != 1) {
                        fName = "";
                    } else {
                        fName = ((Element) authorList.item(j)).getElementsByTagName("ForeName").item(0).getTextContent();
                    }

                    authors.put(fName,lName);
                } catch (NullPointerException e) {
                    failure = true;
                    break;
                }
            }
            if(failure) throw new ArticleParseException("no author : " + pumedId);


            Article a = new Article();
            a.authors = authors;
            a.date = date2;
            a.doi = doi;
            a.journal = journal;
            a.lang = lang;
            a.mAbstract = mabstract;
            a.pubmedid = pumedId;
            a.title = title;

            if(!a.isValid()) throw new ArticleParseException("invalid: " + pumedId);

            successes++;
            return a;
        } catch (NullPointerException e) {
            throw new ArticleParseException(e.getClass() + ", " + e.getMessage());
        } catch (Exception e) {
            throw new ArticleParseException(e.getClass() + ", " + e.getMessage());
        }
    }


    public static void pushBatch(List<Article> articles, Connection db) throws SQLException {

        StringBuilder b = new StringBuilder();
        b.append("INSERT IGNORE INTO `authorships` (`article`, `author`) VALUES ");
        b.append(String.join(",", articles.stream().map(article -> article.toSQL()).collect(Collectors.toList())));
        b.append(";");



        PreparedStatement stmt = db.prepareStatement(b.toString());
        stmt.execute();

        System.out.println(b.toString());

//		INSERT INTO `authorships` (`article`, `author`) VALUES
//		(funcArticleID('La magie du sql', 'doi://moncul', '2015', 'Journal de mon cul', 'mon abstract', 'fr', '5'), funcAuthorID('Nicolas','Harrand'))
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