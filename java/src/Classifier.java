import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import com.google.gson.Gson;

/*
	Classifier:		main system object
	Classification: one per possible document classification
	Document:		A body of text, a classification is required for seeding
	Term:			A token or word associated with a classification
*/

/*

    TODO

    isCommonToken: should you existing information to decide if the given token is common amongst all classification,
        if so, it should be excluded from the seed

    classify: given a body of text, try to classify it using seeded information

*/

class Classifier implements java.io.Serializable {
	HashMap<String,Classification> classifications;
	ArrayList<Classification> classificationsList;
	ArrayList<Term> commonTerms = new ArrayList<Term>();
	ArrayList<String> commonStr = new ArrayList<String>();
    private static Gson gson = new Gson();

    private int _classifierGroupCount = 0;
    private int _classifierGroupThreshold = 5;


	public Classifier () {
		this.classifications = new HashMap<String,Classification>();
		this.classificationsList = new ArrayList<Classification>();
	}
	// using the body of a document only, return an appropriate classification/title
	public String classify ( String body ){ return ""; }
	
	// call to evaluate current classifications for common/important terms
	public void analyseClassifications () {
		this.commonTerms = this.getGlobalTerms();
        for ( Term term : this.commonTerms ){
            this.commonStr.add( term.term );
        }
		/*
        for( Classification c12n : this.classificationsList ) {
			for ( Term term : this.commonTerms ) {
				c12n.terms.remove( term.term );
			}
            /* DEBUG */
            /*
			System.out.println("\n---Terms for c12n: " + c12n.title);
			for ( String c12nt : c12n.terms.keySet().toArray(new String[0]) ) {
				System.out.println( c12nt );
			}
            */
		//}
	}

    public String dumpTerms () {
        String dump = "";

        for( Classification c12n : this.classificationsList ) {
            dump += "\n---Terms for c12n: " + c12n.title + "\n";
            for ( String c12nt : c12n.terms.keySet().toArray(new String[0]) ) {
                if ( this.commonStr.contains( c12nt ) ) continue;
                dump += c12nt + ", ";
            }
            dump += "\n\n";
        }

        return dump;
    }
	
	private HashMap<String,Term> getAllTerms () {
		
		HashMap<String,Term> allTerms = new HashMap<String,Term>();
		
		for( Classification c12n : this.classificationsList ) {
			// foreach term in this classification
			Iterator termIt = c12n.terms.entrySet().iterator();
			while( termIt.hasNext() ){
				Map.Entry termPair = ( Map.Entry ) termIt.next();
				Term term = ( Term ) termPair.getValue();
				
				Term allTerm = allTerms.get( term.term );
				if ( allTerm == null ){
					allTerms.put( term.term, new Term( term.term, 1 ) );
				}
				else {
					allTerm.docCount++;
				}
			}
		}
		return allTerms;
	}
	
	public ArrayList<Term> getGlobalTerms () {
		HashMap<String,Term> allTerms = this.getAllTerms();
		ArrayList<Term> globalTerms = new ArrayList<Term>();
		Iterator allTermsIterator = allTerms.entrySet().iterator();
		
		while ( allTermsIterator.hasNext() ){
			Map.Entry allTermPairs = ( Map.Entry ) allTermsIterator.next();
			Term allTerm = ( Term ) allTermPairs.getValue();
			
			if ( allTerm.docCount == this.classifications.size() ){
				globalTerms.add( allTerm );
			}
		}
		return globalTerms;
	}

    public void seedJSON ( String json ) throws Exception {
        Seed seed = this.gson.fromJson( json, Seed.class );
        this.seed( seed.title, seed.body );
    }

	// enter example document to given title/classification
	public void seed ( String title, String body ) {
		Classification c12n = this.classifications.get( title );
		if ( c12n == null ){
			c12n = new Classification( title );
			this.addClassification( c12n );
		}
		
		String normalisedBody = normaliseBody( body );
		StringTokenizer st = new StringTokenizer( normalisedBody );
		ArrayList<String> tokens = new ArrayList<String>();
		
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if ( token.matches( "^[^a-z0-9]+$" ) )
				continue;
			String [] subTokens = splitJoinedToken( token );
			for ( String subToken : subTokens ){
				if ( ! this.isCommonToken( subToken ) ){
					tokens.add( subToken );
				}
			}
		}
		
		c12n.addDocument( tokens );


        /*
            Every <this._classifierGroupThreshold> documents added ( seeded )
            run the classification analysis automatically
        */
        this._classifierGroupCount++;
        if ( this._classifierGroupCount >= this._classifierGroupThreshold ){
            this.analyseClassifications();
            this._classifierGroupCount = 0;
        }

	}
    private void addClassification ( Classification c12n ){
		this.classifications.put( c12n.title, c12n );
		this.classificationsList.add( c12n );
	}
	
	private boolean isCommonToken ( String token ) {
		return false;
	}

	private String normaliseBody( String body ){
		return body.toLowerCase()
				   .replace( ",", " " )
				   .replaceAll( "[:\\(\\)'\"*]", "" )
				   .replaceAll( "(\\w)\\.\\s", "$1 " );
	}
	private String[] splitJoinedToken( String token ) {
		ArrayList<String> tokens = new ArrayList<String>();
		tokens.add( token );
		String[] joiners = { "-", "/" };		
		for( String join : joiners ){		
			if ( token.contains( join ) ){
				String[] joinTokens = token.split(join);
				for( String joinToken : joinTokens ) {
					tokens.add( joinToken );
				}
			}
		}
		return tokens.toArray(new String[1]);
	}
}

class Classification implements java.io.Serializable {
	public String title;
	public int documents = 0;
	public HashMap<String,Term> terms;
	public Classification ( String title ){
		this.title = title;
		this.terms = new HashMap<String,Term>();
	}
	// add a new token with the given doc count, or if it exists, increment the doccount accordingly
	public void addToken( String token, int docCount ) {
		Term term = this.terms.get( token );
		if ( term == null ) {
			this.terms.put( token, new Term( token, docCount ) );
		}
		else {
			term.docCount += docCount;
		}
	}
	public void addDocument ( ArrayList<String> tokens ){
		this.documents++;
		for ( String token : tokens ){
			this.addToken( token, 1 );
		}
	}
}

class Term implements java.io.Serializable {
	public String term;
	public int docCount;
	public Term ( String term, int docCount ) {
		this.term = term;
		this.docCount = docCount;
	}
}

class Seed implements java.io.Serializable {
    public String title, body;
}
