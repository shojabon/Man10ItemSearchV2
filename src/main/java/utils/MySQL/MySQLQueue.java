package utils.MySQL;


import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class MySQLQueue {

    int executePool;
    int queryPool;
    JavaPlugin plugin;

    ArrayList<LinkedBlockingQueue<String>> executeQueue = new ArrayList<>();
    ArrayList<LinkedBlockingQueue<ThreadedQueryRequest>> queryQueue = new ArrayList<>();

    ConcurrentHashMap<String, ArrayList<MySQLCachedResultSet>> queryResponse = new ConcurrentHashMap<>();

    public final String checker = "";

    Timer timer = new Timer();
    int nextExecuteThread = 0;
    int nextQueryThread = 0;

    public MySQLQueue(int executePool, int queryPool, JavaPlugin plugin){
        this.executePool = executePool;
        this.queryPool = queryPool;
        this.plugin = plugin;

        for(int i = 0; i < executePool; i++){
            executeQueue.add(new LinkedBlockingQueue<>());
            startExecuteThread(i);
        }

        for(int i = 0; i < queryPool; i++){
            queryQueue.add(new LinkedBlockingQueue<>());
            startQueryThread(i);
        }
    }

    public ArrayList<MySQLCachedResultSet> query(String query){
        String responseId = UUID.randomUUID().toString();
        ThreadedQueryRequest request = new ThreadedQueryRequest(query, responseId);
        queryQueue.get(nextQueryThread).add(request);
        nextQueryThread += 1;
        if(nextQueryThread > queryPool-1){
            nextQueryThread = 0;
        }
        try {
            synchronized (checker){
                while(!queryResponse.containsKey(responseId)){
                    checker.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ArrayList<MySQLCachedResultSet> result = queryResponse.get(responseId);
        queryResponse.remove(responseId);
        return result;
    }

    public synchronized void execute(String query){
        executeQueue.get(nextExecuteThread).add(query);
        nextExecuteThread += 1;
        if(nextExecuteThread > executePool-1){
            nextExecuteThread = 0;
        }
    }

    public synchronized void stop(){
        for(LinkedBlockingQueue<String> q: executeQueue){
            q.add("quit");
        }

        for(LinkedBlockingQueue<ThreadedQueryRequest> q: queryQueue){
            q.add(new ThreadedQueryRequest("quit", ""));
        }
    }

    private void startExecuteThread(int id){
        new Thread(()->{
            MySQLAPI manager = new MySQLAPI(plugin);
            while(true){
                try {
                    String take = executeQueue.get(id).take();
                    if(take.equalsIgnoreCase("quit")){
                        manager.close();
                        break;
                    }
                    manager.execute(take);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startQueryThread(int id){
        new Thread(()->{
            MySQLAPI manager = new MySQLAPI(plugin);
            while(true){
                try {
                    ThreadedQueryRequest take = queryQueue.get(id).take();
                    if(take.query.equalsIgnoreCase("quit")){
                        break;
                    }
                    ArrayList<MySQLCachedResultSet> resultSets = manager.cachedQuery(take.query);
                    queryResponse.putIfAbsent(take.id, resultSets);
                    synchronized (checker){
                        checker.notifyAll();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                queryResponse.remove(take.id);
                            }
                        }, 10000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
