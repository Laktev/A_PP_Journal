package app;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

// Handles Google OAuth2 authentication and returns an authorized Drive client.
// Token bundling strategy:
//   - On first run (no tokens/ folder): opens browser once for Google login, saves token to tokens/
//   - On subsequent runs on the same machine: loads token from tokens/ silently
//   - On a NEW machine with no tokens/ folder: checks if StoredCredential is bundled inside the JAR
//     at /app/tokens/StoredCredential — if found, copies it to the local tokens/ folder automatically
//     so the browser login is skipped entirely on every machine you deploy to.
//
// HOW TO BUNDLE THE TOKEN:
//   After your first successful login, copy tokens/StoredCredential into your project resources
//   at: src/app/tokens/StoredCredential  (so it ends up at /app/tokens/StoredCredential in the JAR)
//   From then on, any machine running the app will use that token silently with no browser prompt.
public class DriveServiceUtil {

    private static final String APPLICATION_NAME  = "My Desktop Drive App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR        = "tokens";
    private static final List<String> SCOPES      = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE  = "/app/credentials.json";

    // Classpath path of the bundled token — place StoredCredential here in your resources
    private static final String BUNDLED_TOKEN_PATH = "/app/tokens/StoredCredential";

    public static Drive getDriveService() throws Exception {
        final HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load OAuth client secrets from classpath
        InputStream in = DriveServiceUtil.class.getResourceAsStream(CREDENTIALS_FILE);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // If no local tokens/ folder exists yet, try to seed it from the bundled token in the JAR.
        // This makes the app silent on any new machine without ever opening a browser.
        seedTokenFromClasspathIfNeeded();

        // Build the OAuth flow with the local tokens/ folder as the data store
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // Checks if a bundled StoredCredential exists in the JAR classpath.
    // If the local tokens/ folder is missing or empty, copies the bundled token there.
    // This runs silently — if the bundled token is absent, it does nothing (browser flow proceeds normally).
    private static void seedTokenFromClasspathIfNeeded() {
        try {
            java.io.File tokenDir  = new java.io.File(TOKENS_DIR);
            java.io.File tokenFile = new java.io.File(tokenDir, "StoredCredential");

            // Already have a local token — nothing to do
            if (tokenFile.exists() && tokenFile.length() > 0) return;

            // Try to load the bundled token from inside the JAR
            InputStream bundled = DriveServiceUtil.class.getResourceAsStream(BUNDLED_TOKEN_PATH);
            if (bundled == null) return;  // Not bundled — fall through to normal browser flow

            // Copy bundled token to the local tokens/ folder
            tokenDir.mkdirs();
            Files.copy(bundled, tokenFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[DriveServiceUtil] Seeded token from bundled resources.");

        } catch (Exception e) {
            // Non-fatal — if seeding fails, the normal OAuth browser flow will handle it
            System.err.println("[DriveServiceUtil] Could not seed bundled token: " + e.getMessage());
        }
    }
}