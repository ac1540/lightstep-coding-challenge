import java.io.FileReader;
import java.sql.Timestamp;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;


public class Main {

    //Compile on windows: javac -cp ".;json-simple-1.1.1.jar" Main.java
    //To run on windows: java -cp ".;json-simple-1.1.1.jar" Main
    public static void main(String[] args) throws Exception  {
        //Reading in JSON input
        String filename = null;
        if(args.length > 0) {
            filename = args[0];
        } else {
            filename = "input.json";
        }
        Object obj = new JSONParser().parse(new FileReader(filename));
        JSONArray ja = (JSONArray) obj;
        Map<String, JSONArray> map = new HashMap<String, JSONArray>();
        //Creating a map of transactions to operations
        for(Object o : ja) {
            JSONObject jo = (JSONObject) o;
            String transaction_id = (String) jo.get("transaction_id");
            if (!map.containsKey(transaction_id)) {
                JSONArray new_ja = new JSONArray();
                new_ja.add(jo);
                map.put(transaction_id, new_ja);
            } else {
                map.get(transaction_id).add(jo);
            }
        }
        System.out.println("Longest transaction: " + get_longest_transaction(map));
        System.out.println("Operation with the most errors: " + operation_error(map));
    }
    //Find the longest transaction (in milliseconds)
    private static String get_longest_transaction(Map<String, JSONArray> map) {
        String longest_transaction = "";
        long longest = 0;
        for(String transaction : map.keySet()) {
            JSONArray operations = map.get(transaction);
            Timestamp earliest = null;
            Timestamp latest = null;
            //Go through operations, get earliest and latest timestampts
            for(Object o : operations) {
                JSONObject jo = (JSONObject) o;
                String message = (String) jo.get("message");
                String timestamp = (String) jo.get("timestamp");
                Timestamp ts = Timestamp.valueOf(timestamp);
                //Compare this value with "earliest" start value
                if(message.contains("START")) {
                    if(earliest != null) {
                        if(ts.before(earliest)) {
                            earliest = ts;
                        }
                    } else {
                        earliest = ts;
                    }
                }
                //Compare this value with "latest" start value
                if(message.contains("END")) {
                    if(latest != null) {
                        if(ts.after(latest)) {
                            latest = ts;
                        }
                    } else {
                        latest = ts;
                    }
                }
            }
            //If curr_length is longer than longest, update longest
            long curr_length = latest.getTime() - earliest.getTime();
            if(curr_length > longest) {
                longest = curr_length;
                longest_transaction = transaction;
            }

        }

        return longest_transaction;
    }

    //Method that finds the operation with the most errors
    private static String operation_error(Map<String, JSONArray> map) {
        //method maintains a map of operations to their error counts
        int max_errors = 0;
        String op = "";
        Map<String, Integer> op_error_counts = new HashMap<String, Integer>();
        for(String transaction : map.keySet()) {
            JSONArray operations = map.get(transaction);
            Set<String> error_ops = new HashSet<String>();
            //Go through each operation
            for(Object o : operations) {
                JSONObject jo = (JSONObject) o;
                String level = (String) jo.get("level");
                if(level.equals("ERROR")) {
                    //If operation resulted in an error, increment error count in op_error_counts
                    String operation = (String) jo.get("operation");
                    //Set makes sure that an operation that errors twice in a transaction is not double
                    //counted
                    if(!error_ops.contains(operation)) {
                        error_ops.add(operation);
                        int count = op_error_counts.getOrDefault(operation, 0) + 1;
                        op_error_counts.put(operation, count);
                        //If count > max_errors, update max_errors
                        if(count > max_errors) {
                            max_errors = count;
                            op = operation;
                        }
                    }
                }

            }
        }
        return op;
    }

}
