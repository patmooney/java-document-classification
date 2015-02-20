import java.io.IOException;
import java.io.BufferedReader;
import java.util.HashMap;

public class HttpRequest {

    public String method;
    public String path;
    public String content;
    public HashMap<String,String> headers = new HashMap<String,String>();

    public HttpRequest (){}
    public void parseMethod( String raw ) {
        String[] parts = raw.split(" ");
        this.method = parts[0];
        this.path = parts[1];
    }

    public void parseHeaders( BufferedReader in ) throws IOException {
        String header;
        boolean _parse = true;

        while ( _parse ){
            header = in.readLine();
            if ( header.compareTo("") == 0 ){
                _parse = false; continue;
            }
            
            this._addRawHeader( header );
        }
    }

    public void _addRawHeader( String raw ){
        String[] parts = raw.split(": ");
        this.headers.put( parts[0], parts[1] );
    }
}
