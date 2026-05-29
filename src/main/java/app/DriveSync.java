package app;

//Google API Related-Imports
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

//JDK Imports
import java.io.*;
import java.util.Collections;
import java.util.List;

public class DriveSync {

    // Root folder name in your personal Google Drive
    private static final String ROOT_FOLDER_NAME = "JEntries";

    //Cached Drive client — initialized once, reused for every call
    private static Drive driveService = null;

    //Cached folder IDs
    private static String rootFolderId  = null;  // JEntries/
    private static String usersFolderId = null;  // JEntries/users/

    /* Function:
    Called once at startup from UIOneUserLogin.main() before the login window opens.
    Connects to the service account's GDrive, ensures the root folders exist, and pulls the latest
    users.xml down so the login screen always sees the most current user registry.
    NOTE: Runs synchronously — login window waits for this to finish.
    */
    public static void initAndPullUsersXml() {
        try {
            Drive svc = getService();
            ensureRootFolders(svc);
            pullUsersXml(svc);
        } catch (Exception e) {
            System.err.println("[DriveSync] initAndPullUsersXml failed: " + e.getMessage());
        }
    }

    /* Function:
    Uploads the local users.xml to GDrive. Called from XMLUserStorage after every write
    so the registry stays in sync.
    */
    public static void pushUsersXml() {
        runAsync(() -> {
            try {
                Drive svc = getService();
                ensureRootFolders(svc);
                java.io.File local = new java.io.File(System.getProperty("user.dir"), "users.xml");
                uploadOrUpdate(svc, rootFolderId, "users.xml", local, "application/xml");
            } catch (Exception e) {
                System.err.println("[DriveSync] pushUsersXml failed: " + e.getMessage());
            }
        });
    }

    /* Function:
    Uploads a single entry XML file to JEntries/users/<username>/ on GDrive.
    Called from UISixEntryEdit.saveEntry() after every successful save.
    NOTE: Creates the user sub-folder on Drive if it doesn't exist yet.
    */
    public static void pushEntry(String username, String entryFileName, String localFolder) {
        runAsync(() -> {
            try {
                Drive svc = getService();
                ensureRootFolders(svc);
                String userDriveFolder = ensureUserFolder(svc, username);
                java.io.File local = new java.io.File(localFolder, entryFileName + ".xml");
                uploadOrUpdate(svc, userDriveFolder, entryFileName + ".xml", local, "application/xml");
            } catch (Exception e) {
                System.err.println("[DriveSync] pushEntry failed: " + e.getMessage());
            }
        });
    }

    /* Function:
    Removes an entry XML file from the GDrive folder.
    Called from UIFourEntryListViewer after the local file is successfully deleted.
    */
    public static void deleteEntry(String username, String entryFileName) {
        runAsync(() -> {
            try {
                Drive svc = getService();
                String userDriveFolder = findFolderId(svc, usersFolderId, username);
                if (userDriveFolder == null) return;
                String fileId = findFileId(svc, userDriveFolder, entryFileName + ".xml");
                if (fileId != null) svc.files().delete(fileId).execute();
            } catch (Exception e) {
                System.err.println("[DriveSync] deleteEntry failed: " + e.getMessage());
            }
        });
    }

    /* Function:
    Pulls all entry files from GDrive folder into the local folder.
    Called right after login so the user always opens their latest entries.
    Skips empty Drive files (size == 0) to avoid HTTP 416 errors.
    */
    public static void pullUserEntries(String username, String localFolderPath) {
        try {
            Drive svc = getService();
            ensureRootFolders(svc);
            String userDriveFolder = ensureUserFolder(svc, username);

            // Request the 'size' field so we can skip empty files before attempting download
            FileList result = svc.files().list()
                    .setQ("'" + userDriveFolder + "' in parents and trashed=false")
                    .setFields("files(id,name,size)")
                    .execute();

            java.io.File localDir = new java.io.File(localFolderPath);
            if (!localDir.exists()) localDir.mkdirs();

            for (File driveFile : result.getFiles()) {
                // Skip empty files — downloading a 0-byte file triggers HTTP 416
                Long size = driveFile.getSize();
                if (size == null || size == 0L) {
                    System.out.println("[DriveSync] Skipping empty file on Drive: " + driveFile.getName());
                    continue;
                }

                java.io.File localFile = new java.io.File(localDir, driveFile.getName());
                try (OutputStream os = new FileOutputStream(localFile)) {
                    svc.files().get(driveFile.getId()).executeMediaAndDownloadTo(os);
                }
            }
        } catch (Exception e) {
            System.err.println("[DriveSync] pullUserEntries failed: " + e.getMessage());
        }
    }

    // Updates the filename of an entry on GDrive when the subject changes on save.
    // Called from UISixEntryEdit.saveEntry() when it detects the subject was edited.
    public static void renameEntry(String username, String oldEntryName, String newEntryName) {
        runAsync(() -> {
            try {
                Drive svc = getService();
                String userDriveFolder = findFolderId(svc, usersFolderId, username);
                if (userDriveFolder == null) return;
                String fileId = findFileId(svc, userDriveFolder, oldEntryName + ".xml");
                if (fileId == null) return;
                File meta = new File();
                meta.setName(newEntryName + ".xml");
                svc.files().update(fileId, meta).execute();
            } catch (Exception e) {
                System.err.println("[DriveSync] renameEntry failed: " + e.getMessage());
            }
        });
    }

    /* Function:
    Renames the user's folder on GDrive when the username is changed in account settings.
    Called from XMLUserStorage.updateUser() after the local folder is successfully renamed.
    */
    public static void renameUserFolder(String oldUsername, String newUsername) {
        runAsync(() -> {
            try {
                Drive svc = getService();
                ensureRootFolders(svc);
                String folderId = findFolderId(svc, usersFolderId, oldUsername);
                if (folderId == null) return;
                File meta = new File();
                meta.setName(newUsername);
                svc.files().update(folderId, meta).execute();
            } catch (Exception e) {
                System.err.println("[DriveSync] renameUserFolder failed: " + e.getMessage());
            }
        });
    }

    /* Function:
    Deletes the user's entire folder from GDrive when the account is deleted.
    Called from XMLUserStorage.deleteUser() after the local folder is wiped.
    */
    public static void deleteUserFolder(String username) {
        runAsync(() -> {
            try {
                Drive svc = getService();
                String folderId = findFolderId(svc, usersFolderId, username);
                if (folderId != null) svc.files().delete(folderId).execute();
            } catch (Exception e) {
                System.err.println("[DriveSync] deleteUserFolder failed: " + e.getMessage());
            }
        });
    }

    // Lazily initializes the Drive client the first time it's needed and caches it.
    // Synchronized so concurrent background threads don't race to create multiple instances.
    private static synchronized Drive getService() throws Exception {
        if (driveService == null) {
            driveService = DriveServiceUtil.getDriveService();
        }
        return driveService;
    }

    // Runs a task on a daemon thread so it never blocks the Swing UI.
    // Daemon threads are automatically killed when the main thread exits — no cleanup needed.
    private static void runAsync(Runnable task) {
        Thread t = new Thread(task, "DriveSync-worker");
        t.setDaemon(true);
        t.start();
    }


    // Ensures JEntries/ and JEntries/users/ exist in your Drive, then caches their IDs.
    // Called at the top of every operation so the folder structure is always ready.
    private static void ensureRootFolders(Drive svc) throws IOException {
        if (rootFolderId  == null) rootFolderId  = findOrCreateFolder(svc, "root", ROOT_FOLDER_NAME);
        if (usersFolderId == null) usersFolderId = findOrCreateFolder(svc, rootFolderId, "users");
    }

    // Ensures JEntries/users/<username>/ exists and returns its Drive folder ID.
    // Creates the folder if it's missing — happens automatically on first login or first save.
    private static String ensureUserFolder(Drive svc, String username) throws IOException {
        ensureRootFolders(svc);
        return findOrCreateFolder(svc, usersFolderId, username);
    }

    // Searches for a folder by name inside a parent. Creates and returns it if not found.
    private static String findOrCreateFolder(Drive svc, String parentId, String name) throws IOException {
        String existing = findFolderId(svc, parentId, name);
        if (existing != null) return existing;

        File meta = new File();
        meta.setName(name);
        meta.setMimeType("application/vnd.google-apps.folder");
        meta.setParents(Collections.singletonList(parentId));
        File created = svc.files().create(meta).setFields("id").execute();
        return created.getId();
    }

    // Returns the Drive folder ID of <name> inside <parentId>, or null if not found.
    // Apostrophes in the name are escaped to avoid breaking the query string.
    private static String findFolderId(Drive svc, String parentId, String name) throws IOException {
        if (parentId == null) return null;
        String q = "'" + parentId + "' in parents"
                + " and name='" + name.replace("'", "\\'") + "'"
                + " and mimeType='application/vnd.google-apps.folder'"
                + " and trashed=false";
        FileList list = svc.files().list().setQ(q).setFields("files(id)").execute();
        List<File> files = list.getFiles();
        return (files != null && !files.isEmpty()) ? files.get(0).getId() : null;
    }

    // Returns the Drive file ID of <name> inside <parentId>, or null if not found.
    private static String findFileId(Drive svc, String parentId, String name) throws IOException {
        if (parentId == null) return null;
        String q = "'" + parentId + "' in parents"
                + " and name='" + name.replace("'", "\\'") + "'"
                + " and trashed=false";
        FileList list = svc.files().list().setQ(q).setFields("files(id)").execute();
        List<File> files = list.getFiles();
        return (files != null && !files.isEmpty()) ? files.get(0).getId() : null;
    }

    // Uploads a local file to Drive. If a file with the same name already exists in the parent
    // folder, it's updated in-place so the file ID stays stable.
    // Does nothing if the local file doesn't exist or is empty.
    private static void uploadOrUpdate(Drive svc, String parentId, String driveName,
                                       java.io.File localFile, String mimeType) throws IOException {
        if (!localFile.exists() || localFile.length() == 0) return;

        FileContent content = new FileContent(mimeType, localFile);
        String existingId   = findFileId(svc, parentId, driveName);

        if (existingId != null) {
            svc.files().update(existingId, new File(), content).execute();
        } else {
            File meta = new File();
            meta.setName(driveName);
            meta.setParents(Collections.singletonList(parentId));
            svc.files().create(meta, content).setFields("id").execute();
        }
    }

    // Downloads the Drive copy of users.xml and overwrites the local file.
    // Skips the download if the Drive file is empty (size == 0) to avoid HTTP 416.
    // If no Drive copy exists yet (first-ever launch), does nothing.
    private static void pullUsersXml(Drive svc) throws IOException {
        // Request size alongside id so we can guard against empty files
        String q = "'" + rootFolderId + "' in parents"
                + " and name='users.xml'"
                + " and trashed=false";
        FileList list = svc.files().list().setQ(q).setFields("files(id,size)").execute();
        List<File> files = list.getFiles();

        if (files == null || files.isEmpty()) return;  // No Drive copy yet — first launch

        File driveFile = files.get(0);
        Long size = driveFile.getSize();

        if (size == null || size == 0L) {
            // File exists on Drive but is empty — skip download, keep local copy as-is
            System.out.println("[DriveSync] users.xml on Drive is empty — skipping pull.");
            return;
        }

        java.io.File local = new java.io.File(System.getProperty("user.dir"), "users.xml");
        try (OutputStream os = new FileOutputStream(local)) {
            svc.files().get(driveFile.getId()).executeMediaAndDownloadTo(os);
        }
    }
}