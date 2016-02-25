package internet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

public class ThingSpeak {

    public static String getHTML(String urlToRead) throws Exception {

        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (UnknownHostException e) {
            System.out.println("Impossibile inviare dati a thingspeak.");
        }
        catch (SocketException e) {
            System.out.println("Impossibile inviare dati a thingspeak.");
        }
        return result.toString();
    }
}
