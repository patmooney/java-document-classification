class HttpResponse {
    public int statusCode = 200;
    public String content,
                  contentType = "text/plain",
                  statusMessage = "OK";
    
    public int contentLength () {
        return this.content.length();
    }
    public HttpResponse ( String type, String content ){
        this.content = content;
        this.contentType = type;
    }
}
