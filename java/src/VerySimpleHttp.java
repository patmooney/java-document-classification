import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class VerySimpleHttp {
    String classifierFileName = "/home/patrick/dev/job-classification/java/src/classifier.ser";
    Classifier classifier = null;

    public static void main ( String[] args ) throws Exception {
        int port = 47654;
        if ( args.length > 0 ){
            port = Integer.parseInt(args[0]);
        }
        VerySimpleHttp server = new VerySimpleHttp( port );

        try {
            FileInputStream fileIn = new FileInputStream( server.classifierFileName );
            ObjectInputStream in = new ObjectInputStream(fileIn);
            server.classifier = (Classifier) in.readObject();
            in.close();
            fileIn.close();
        }
        catch(IOException i) {
            i.printStackTrace();
        }
        catch(ClassNotFoundException c){
            System.out.println("Classifier class not found");
        }

        if ( server.classifier == null ) server.classifier = new Classifier();

        System.out.println( "Starting on port: " + port );
        server.go();
    }

    private static ServerSocket listenerSocket = null; 

    public VerySimpleHttp ( int port ) {
        try {
            this.listenerSocket = new ServerSocket(port);
        } catch ( Exception e ){
            e.printStackTrace();
        }
    }

    public void go () throws Exception {
        while ( true ){
            Socket socket = listenerSocket.accept();
            new Worker(new RequestHandler(socket));
        }
    }

    private class Worker {
        public Worker(Runnable task) {
           new Thread(task).start();
        }
    }

    private class RequestHandler implements Runnable {
        private Socket socket = null;
        
        public RequestHandler (Socket socket){
            this.socket = socket;
        }

        @Override
        public void run()
        {
            BufferedReader in = null;
            PrintStream out = null;
            try {
                // read request from the client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintStream(new BufferedOutputStream(socket.getOutputStream()));
                HttpRequest httpreq = parseRequest(in, out);

                switch( httpreq.path ){
                    case "/": this._respond( out, this.routeIndex( httpreq ) ); break;
                    case "/seed": this._respond( out, this.routeSeed( httpreq ) ); break;
                    case "/analyse": this._respond( out, this.routeAnalyse( httpreq ) ); break;
                    case "/save": this._respond( out, this.routeSaveClassifier( httpreq ) ); break;
                    case "/common": this._respond( out, this.routeDumpCommon( httpreq ) ); break;
                    case "/classify": this._respond( out, this.routeClassify( httpreq ) ); break;
                }


            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                close(in);
                close(out);
                close(socket);
            }
        }

        private void close ( BufferedReader in ){
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
        private void close(PrintStream out) {
            if (out != null) out.close();
        }
        private void close ( Socket in ){
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
        private void _respond( PrintStream out, HttpResponse resp ){
            out.print("HTTP/1.1 " + resp.statusCode + " " + resp.statusMessage + "\r\n");
            out.print("Content-Type: " + resp.contentType + "\r\n");
            out.print("Content-Length: " + resp.contentLength() + "\r\n\r\n");
            out.print( resp.content );
            out.flush();
        }

        private HttpResponse routeIndex( HttpRequest req ) {
            HttpResponse resp = new HttpResponse( "application/json", "{ \"name\": \"patrick\" }" );
            return resp;
        }

        private HttpResponse routeSeed( HttpRequest req ) {
            HttpResponse resp = new HttpResponse( "text/plain", "" );
            try {
                classifier.seedJSON( req.content );
            }
            catch ( Exception e ){
                resp.content = e.getMessage();
            }
            return resp;
        }

        private HttpResponse routeAnalyse( HttpRequest req ){
            String content = classifier.dumpTerms();
            return new HttpResponse( "text/plain", content );
        }

        private HttpResponse routeDumpCommon( HttpRequest req ){
            classifier.analyseClassifications();
            String content = "";
            for ( String str : classifier.commonStr ){
                content += str + ", ";
            }
            return new HttpResponse( "text/plain", content );
        }

        private HttpResponse routeSaveClassifier( HttpRequest req ){
            try {
                 FileOutputStream fileOut =
                 new FileOutputStream(classifierFileName);
                 ObjectOutputStream out = new ObjectOutputStream(fileOut);
                 out.writeObject( classifier );
                 out.close();
                 fileOut.close();
                 System.out.printf("Serialized data is saved in " + classifierFileName);
            }
            catch(IOException i) {
                i.printStackTrace();
            }
            return new HttpResponse( "text/plain", "" );
        }

        private HttpResponse routeClassify ( HttpRequest req ){
            String content = classifier.classify( req.content );
            return new HttpResponse( "text/plain", content );
        }
    }

    private HttpRequest parseRequest( BufferedReader in, PrintStream out ) throws IOException {
        String line;
        HttpRequest httpreq = new HttpRequest();

        httpreq.parseMethod( in.readLine() );
        httpreq.parseHeaders( in );

        String expected = httpreq.headers.get("Expect");
        if ( expected != null ){
            out.print("HTTP/1.1 100 Continue\r\n\r\n");
            out.flush();
        }

        if ( httpreq.method.toLowerCase().equals("post") ){
            String contentLengthHeader = httpreq.headers.get("Content-Length");
            if ( contentLengthHeader != null ){
                int content_length = Integer.parseInt( httpreq.headers.get("Content-Length") );

                char[] chs = new char[content_length];
                in.read( chs, 0, content_length );
                httpreq.content = new String( chs );
            }
        }

        return httpreq;
    }

    private long _time () {
        return System.currentTimeMillis() / 100;
    }

}
