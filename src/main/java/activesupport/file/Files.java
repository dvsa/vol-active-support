package activesupport.file;

import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;

import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import activesupport.config.Configuration;

public class Files {
    /**
     * Deletes Folder with all of its content
     *
     * @param folder path to folder which should be deleted
     */
    public static void deleteFolderAndItsContent(final Path folder) throws IOException {
        java.nio.file.Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (java.nio.file.Files.exists(file)) {
                    java.nio.file.Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                if ((new File(dir.toString()).exists())) {
                    java.nio.file.Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static File createDirectory(@NotNull String path) {
        return createFolder(path);
    }

    public static File createFolder(@NotNull String path) {
        File folder = new File(path);

        if (!folder.exists()) {
            folder.mkdir();
        }

        return folder;
    }

    public static void write(@NotNull String path, @NotNull String contents) throws IOException {
        Files.write(Paths.get(path), contents);
    }

    public static void write(@NotNull Path path, @NotNull String contents) throws IOException {
        File file = path.toFile();

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] strToBytes = contents.getBytes();
            outputStream.write(strToBytes);
        }
    }

    public static void delete(@NotNull String file) {
        Path path = Paths.get(file);

        if (path.toFile().exists()) {
            path.toFile().delete();
        }
    }

    public static boolean checkFileContainsText(String fileLocation, String chosenString) throws IOException {
        boolean fileContainsString = false;
        File template = new File(fileLocation);

        BufferedReader br = new BufferedReader(new FileReader(template));
        String readString;

        while ((readString = br.readLine()) != null) {
            System.out.println(readString);
            fileContainsString = readString.contains(chosenString);
            if (fileContainsString) {
                break;
            }
        }
        br.close();
        return fileContainsString;
    }

    public static File getDownloadedFile(String downloadDirectory, String filenameRegex) throws FileNotFoundException {
        Config config = new Configuration().getConfig();
        File directory = new File(config.getString(downloadDirectory));
        File[] files;

        long finish = System.currentTimeMillis() + 10000;
        do {
            files = directory.listFiles((FileFilter) new RegexFileFilter(filenameRegex));
        } while (files.length == 0 && System.currentTimeMillis() < finish);

        if (files.length == 0) {
            throw new FileNotFoundException();
        } else {
            long lastModified;
            long prevLastModified;

            do {
                prevLastModified = files[0].lastModified();
                lastModified = files[0].lastModified();
            } while (prevLastModified != lastModified);

            return files[0];
        }
    }
}
