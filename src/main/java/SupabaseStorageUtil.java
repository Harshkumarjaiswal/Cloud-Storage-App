import okhttp3.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class SupabaseStorageUtil {
    private static String SUPABASE_URL;
    private static String SUPABASE_KEY;
    private static final String BUCKET = "user-files";
    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    static {
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(Path.of("supabase.properties")));
            SUPABASE_URL = props.getProperty("supabase.url");
            SUPABASE_KEY = props.getProperty("supabase.key");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Supabase config: " + e.getMessage());
        }
    }

    public static boolean uploadFile(String userId, String fileName, byte[] fileContent) throws IOException {
        String path = userId + "/" + fileName;
        String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + path;
        
        System.out.println("Uploading to URL: " + url);
        System.out.println("File size: " + fileContent.length + " bytes");
        System.out.println("Using bucket: " + BUCKET);
        
        // URL encode the path to handle special characters
        String encodedPath = java.net.URLEncoder.encode(path, "UTF-8");
        String encodedUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + encodedPath;
        
        RequestBody body = RequestBody.create(fileContent, MediaType.parse("application/octet-stream"));
        Request request = new Request.Builder()
                .url(encodedUrl)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/octet-stream")
                .header("x-upsert", "true")
                .put(body)
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response code: " + response.code());
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("Upload failed. Response: " + errorBody);
                System.err.println("Request URL: " + encodedUrl);
                System.err.println("Bucket: " + BUCKET);
                System.err.println("Path: " + path);
                System.err.println("Headers: " + request.headers());
                return false;
            }
            return true;
        }
    }

    public static byte[] downloadFile(String userId, String fileName) throws IOException {
        String path = userId + "/" + fileName;
        String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + path;
        
        System.out.println("Downloading from URL: " + url);
        System.out.println("Using bucket: " + BUCKET);
        System.out.println("Path: " + path);
        
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .get()
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response code: " + response.code());
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("Download failed. Response: " + errorBody);
                System.err.println("Request URL: " + url);
                System.err.println("Bucket: " + BUCKET);
                System.err.println("Path: " + path);
                throw new IOException("Failed to download: " + errorBody);
            }
            return Objects.requireNonNull(response.body()).bytes();
        }
    }

    public static boolean deleteFile(String userId, String fileName) throws IOException {
        String path = userId + "/" + fileName;
        String url = SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + path;
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    // Optionally, you can add a method to create a folder (Supabase treats folders as prefixes)
    public static boolean createFolder(String userId, String folderName) throws IOException {
        // Create an empty file to represent the folder
        return uploadFile(userId, folderName + "/.keep", new byte[0]);
    }

    public static String shareFile(String userId, String fileName) throws IOException {
        String path = userId + "/" + fileName;
        String url = SUPABASE_URL + "/storage/v1/object/sign/" + BUCKET + "/" + path;
        
        System.out.println("Generating share URL for: " + url);
        System.out.println("Using bucket: " + BUCKET);
        System.out.println("Path: " + path);
        
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .post(RequestBody.create(new byte[0]))
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response code: " + response.code());
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("Share URL generation failed. Response: " + errorBody);
                throw new IOException("Failed to generate share URL: " + errorBody);
            }
            
            // Parse the response to get the signed URL
            String responseBody = response.body().string();
            // The response will contain a signed URL that we can use for sharing
            return responseBody;
        }
    }

    public static List<FileInfo> listFiles(String userId) throws IOException {
        List<FileInfo> files = new ArrayList<>();
        String url = SUPABASE_URL + "/storage/v1/object/list/" + BUCKET + "/" + userId;
        
        System.out.println("Listing files from URL: " + url);
        
        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .get()
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("List files failed. Response: " + errorBody);
                throw new IOException("Failed to list files: " + errorBody);
            }
            
            String responseBody = response.body().string();
            System.out.println("List files response: " + responseBody);
            
            // Parse the JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            
            if (root.isArray()) {
                for (JsonNode node : root) {
                    String name = node.get("name").asText();
                    // Skip the .keep file
                    if (name.equals(".keep")) continue;
                    
                    long size = node.has("metadata") && node.get("metadata").has("size") 
                        ? node.get("metadata").get("size").asLong() 
                        : 0;
                    
                    String lastModified = node.has("metadata") && node.get("metadata").has("lastModified") 
                        ? node.get("metadata").get("lastModified").asText() 
                        : "";
                    
                    files.add(new FileInfo(name, size, lastModified));
                }
            }
        }
        
        return files;
    }
} 