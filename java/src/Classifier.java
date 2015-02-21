import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

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

    // allows serialised objects to be de-serialised against any new versions
    static final long serialVersionUID = 2143854814331514662L;

    // precisionFactor
    //  The lower the number, the less special-terms per classification
    private final double precisionFactor = 0.2;
	
    HashMap<String,Classification> classifications;
	ArrayList<Classification> classificationsList;
	ArrayList<Term> commonTerms = new ArrayList<Term>();
	ArrayList<String> commonStr = new ArrayList<String>();
    private static Gson gson = new Gson();

    private int _classifierGroupCount = 0;
    private int _classifierGroupThreshold = 50;


	public Classifier () {
		this.classifications = new HashMap<String,Classification>();
		this.classificationsList = new ArrayList<Classification>();
	}
	
    // using the body of a document only, return an appropriate classification/title
	public String classify ( String body ){

        String content = "";

        HashMap<String,ClassifierMatch> foundClass = new HashMap<String,ClassifierMatch>();

		String normalisedBody = normaliseBody( body );
		StringTokenizer st = new StringTokenizer( normalisedBody );
		ArrayList<String> tokens = new ArrayList<String>();
		
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if ( token.matches( "^[^a-z0-9]+$" ) )
				continue;
			String [] subTokens = splitJoinedToken( token );
			for ( String subToken : subTokens ){
				if ( ! this.commonStr.contains( subToken ) ){

                    System.out.println( subToken );

					tokens.add( subToken );
				}
			}
		}

        for( Classification c12n : this.classificationsList ) {
			// foreach term in this classification
			Iterator termIt = c12n.specialTerms.entrySet().iterator();
			while( termIt.hasNext() ){
				Map.Entry termPair = ( Map.Entry ) termIt.next();
				Term term = ( Term ) termPair.getValue();
                
                for ( String token : tokens ){
                    if ( token.equals( term.term ) ){
                        ClassifierMatch c12nm = foundClass.get( c12n.title );
                        if ( c12nm == null ){
                            c12nm = new ClassifierMatch( c12n );
                        }
                        c12nm.count++;
                        foundClass.put( c12n.title, c12nm );
                    }
                }
            }
        }

        ArrayList<ClassifierMatch> valuesList = new ArrayList<ClassifierMatch>(foundClass.values());
        Collections.sort( valuesList, new CustomComparator());
        for ( ClassifierMatch c12nm : valuesList.subList( 0, 5 ) ){
            String title = c12nm.c12n.title;
            int count = (int) c12nm.count;
            content += count + " : " + title + "\n";
        }
        return content;
    }

    public class CustomComparator implements Comparator<ClassifierMatch> {
        @Override
        public int compare(ClassifierMatch o1, ClassifierMatch o2) {
            // descending order
            return o2.count - o1.count;
        }
    }
	
	// call to evaluate current classifications for common/important terms
	public void analyseClassifications () {
		this.commonTerms = this.getGlobalTerms();
        this.commonStr.clear();
        for ( Term term : this.commonTerms ){
            this.commonStr.add( term.term );
        }

        for( Classification c12n : this.classificationsList ) {
            // foreach term in this classification
            c12n.specialTerms = new HashMap<String, Term>();
            Iterator termIt = c12n.terms.entrySet().iterator();
            while( termIt.hasNext() ){
                Map.Entry termPair = ( Map.Entry ) termIt.next();
                Term term = ( Term ) termPair.getValue();
                if ( ! this.commonStr.contains( term.term ) ){
                    c12n.specialTerms.put( term.term, term );
                }
            }
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
            int termCount = c12n.terms.size();
            dump += "\n---Terms for c12n: " + c12n.title + " (" + termCount+")\n";

            for ( String strTerm : this.commonStr ) {
                if ( c12n.terms.containsKey( strTerm ) ) termCount--;
            }

            for ( String c12nt : c12n.terms.keySet().toArray(new String[0]) ) {
                if ( this.commonStr.contains( c12nt ) ) continue;
                if ( c12n.terms.get( c12nt ).docCount > 50 )
                    //System.out.println( c12nt + ": " + c12n.terms.get( c12nt ).docCount + " - " + ((int)( termCount / 20 )) );
                if ( c12n.terms.get( c12nt ).docCount > ((int)( termCount / 20 )) ){
                    dump += c12nt + ", ";
                }
            }
            dump += "\n\n";
        }

        dump += "\n-- COMMON TERMS --\n";
        for ( String common : this.commonStr ){
            dump += common + ", ";
        }

        dump += "\n\n";

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
			
			if ( allTerm.docCount > (int)( this.classifications.size() * this.precisionFactor ) ){
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
				if ( ! this.commonStr.contains( subToken ) ){
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
				   .replaceAll( ",", " " )
				   .replaceAll( "[:\\(\\)'\"*]", "" )
				   .replaceAll( "(\\w)\\.", "$1 " )
                   .replaceAll( "•", "" )
                   .replaceAll( "[^\\w\\s](\\w)", "$1" )
                   .replaceAll( "[“”]", "" )
                   .replaceAll( "’", "'" )
                   .replaceAll( "\\-", "" )
                   .replaceAll( "\\.\\.\\.", "" );
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

    static final long serialVersionUID = 5322997595420932400L;

	public String title;
	public int documents = 0;
	public HashMap<String,Term> terms;
	public HashMap<String,Term> specialTerms;
	public Classification ( String title ){
		this.title = title;
		this.terms = new HashMap<String,Term>();
		this.specialTerms = new HashMap<String,Term>();
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

class ClassifierMatch {
    public Classification c12n;
    public Integer count;
    public ClassifierMatch ( Classification c12n ) {
        this.c12n = c12n;
        this.count = 0;
    }
}
