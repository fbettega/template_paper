package ml.serveurduplacard;

import java.util.Map;
import java.util.stream.Collectors;

public class Article {
	public Map<String,String> authors;
	public String date;
	public String journal;
	public String doi;
	public String pubmedid;
	public String title;
	public String lang;
	public String mAbstract;


	public String toString() {
		String authorsStr = String.join(",", authors.entrySet().stream().map((e) -> e.getKey() + " " + e.getValue()).collect(Collectors.<String>toList()));
		return pubmedid + ": authors: " + authorsStr + ", date: " + date + ", journal: " + journal + ", doi: " + (doi == null ? "NULL" : doi) + ", title: " + title + ", lang: " + lang;
	}

	public boolean isValid() {
		return authors != null &&
				!authors.isEmpty() &&
				date != null &&
				journal != null &&
				pubmedid != null &&
				title != null &&
				lang != null &&
				mAbstract != null;
	}

	public String toSQL() {
		StringBuilder b = new StringBuilder();
		boolean isFirst = true;
		for(Map.Entry<String,String> e: authors.entrySet()) {

			if(isFirst) isFirst = false;
			else b.append(",");

			b.append("(funcArticleID('");
			b.append(title.replace("'",""));
			b.append("',");
			b.append(doi == null ? "NULL" : "'" + doi.replace("'","") + "'");
			b.append(",");
			b.append(date.replace("'",""));
			b.append(",'");
			b.append(journal.replace("'",""));
			b.append("','");
			b.append(mAbstract.replace("'",""));
			b.append("','");
			b.append(lang.replace("'",""));
			b.append("',");
			b.append(pubmedid.replace("'",""));
			b.append("),funcAuthorID('");
			b.append(e.getKey().replace("'",""));
			b.append("','");
			b.append(e.getValue().replace("'",""));
			b.append("'))");

		}
		return b.toString();
	}
}
