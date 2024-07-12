package org.nkumar.utilities.huge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Main {
    public static void main(String[] args) throws Exception {
        Options options = parseArgs(args);
        System.out.println("Scanning " + options.root.getAbsolutePath() + " for files or dirs larger than " + options.maxMBs + " MB");
        printOutputFormat();
        long BYTES_IN_MB = 1024 * 1024L;
        FileData fileData = locateHugeFiles(options.root, options.maxMBs * BYTES_IN_MB);
        System.out.println("Total Size: " + ((fileData.getSize() / BYTES_IN_MB) + " MB"));
    }

    //<editor-fold desc="parseArgs">
    private static final class Options {
        final File root;
        final int maxMBs;

        public Options(File root, int maxMBs) {
            this.root = root;
            this.maxMBs = maxMBs;
        }
    }

    private static void printUsage(int exitCode) {
        System.out.println("java -jar huge.jar [-root rootdir] [-max <size in MB>] [-h]");
        System.out.println("Default root dir is the current directory");
        System.out.println("Default max size is 100 MB");
        printOutputFormat();
        System.exit(exitCode);
    }


    private static Options parseArgs(String[] args) throws Exception {
        File root = new File(".");
        int maxMBs = 100;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsage(0);
            } else if ("-root".equals(arg)) {
                i++;
                if (args.length == i) {
                    printUsage(1);
                }
                root = new File(args[i]);
            } else if ("-max".equals(arg)) {
                i++;
                if (args.length == i) {
                    printUsage(1);
                }
                try {
                    maxMBs = Integer.parseInt(args[i]);
                } catch (NumberFormatException ignore) {
                    printUsage(1);
                }
            }
        }

        if (!root.isDirectory()) {
            System.err.println(root.getAbsolutePath() + " is not a directory");
            printUsage(1);
        }
        if (!root.exists()) {
            System.err.println(root.getAbsolutePath() + " does not exist");
            printUsage(1);
        }
        root = root.getCanonicalFile();
        return new Options(root, maxMBs);
    }
    //</editor-fold>

    private static FileData locateHugeFiles(File dir, long maxSize) {
        FileData fileData = new FileData(dir);
        List<File> files = listFiles(dir);
        for (File file : files) {
            if (file.isFile()) {
                FileData nestedFileData = new FileData(file);
                //this is necessary if the individual file size if more than the limit
                nestedFileData.printIfOverLimit(maxSize);
                fileData.addNestedFileSize(nestedFileData);
            } else if (file.isDirectory()) {
                FileData nestedFileData = locateHugeFiles(file, maxSize);
                fileData.addNestedFileSize(nestedFileData);
            }
        }
        fileData.printIfOverLimit(maxSize);
        return fileData;
    }

    private static final class FileData {
        final File file;
        //size of this file or directory
        long size;
        //size of files or dirs within this dir whose huge size has already been reported to the user
        long reportedSize;
        long lastModified;
        boolean printed = false;

        FileData(File file) {
            this.file = file;
            if (file.isFile()) {
                this.size = file.length();
                this.lastModified = file.lastModified();
            }
        }

        void addNestedFileSize(FileData fileData) {
            if (fileData == null) {
                throw new IllegalStateException("Nested file data is null");
            }
            if (printed) {
                throw new IllegalStateException("Nested info cannot be added after being printed");
            }
            size += fileData.size;
            reportedSize += fileData.reportedSize;
            if (fileData.lastModified > lastModified) {
                lastModified = fileData.lastModified;
            }
        }

        void printIfOverLimit(long limit) {
            if (size - reportedSize >= limit) {
                System.out.printf("%6d MB %2d MO %s\n", (int) (size / (1024 * 1024L)),
                        (int) ((System.currentTimeMillis() - lastModified) / (1000L * 60 * 60 * 24 * 30)),
                        file.getAbsolutePath());
                reportedSize = size;
            }
            printed = true;
        }

        public long getSize() {
            return size;
        }
    }

    //<editor-fold desc="listFiles">

    /**
     * List files/directories in a directory.
     * Nested files which cannot be read or are links are ignored.
     * Files returned in alphabetical order.
     *
     * @return non-null list of children of the dir
     */
    private static List<File> listFiles(File dir) {
        assert dir != null && dir.isDirectory();
        File[] files = dir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files).filter(f -> f.canRead() && isNotLinkFile(f.toPath())).sorted(
                Comparator.comparing(File::getName)).collect(Collectors.toList());
    }

    private static boolean isNotLinkFile(Path path) {
        if (Files.isSymbolicLink(path)) {
            return false;
        }
        try {
            final boolean junction = path.compareTo(path.toRealPath()) != 0;
            if (junction) {
                return false;
            }
        } catch (IOException ignore) {
        }
        return true;
    }
    //</editor-fold>

    private static void printOutputFormat() {
        System.out.println("Output Format: <size in MB> <time since last modification in months> <absolute file path>");
    }
}
