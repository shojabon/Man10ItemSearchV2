package utils.MySQL;

public class ThreadedQueryRequest {

    public String query;
    public String id;

    public ThreadedQueryRequest(String query, String id){
        this.query = query;
        this.id = id;
    }

}
