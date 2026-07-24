import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeepSeekPingTest {
    public static void main(String[] args) throws Exception {
        // 把下面这里换成你真实的 sk-xxx 密钥
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Please set DEEPSEEK_API_KEY before running this test.");
        }
        String url = "https://api.deepseek.com/chat/completions";
        String json = "{\"model\": \"deepseek-chat\", \"messages\": [{\"role\": \"user\", \"content\": \"你好！猜猜我的身份\"}]}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        System.out.println("--- Sending Request ---");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
