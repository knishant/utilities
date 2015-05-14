package org.nkumar.utilities.huge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public final class Main
{
    public static void main(String[] args) throws Exception
    {
        File root = new File(".");
        int maxMBs = 100;
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg))
            {
                printUsage();
            }
            else if ("-root".equals(arg))
            {
                i++;
                if (args.length == i)
                {
                    printUsage();
                }
                root = new File(args[i]);
            }
            else if ("-max".equals(arg))
            {
                i++;
                if (args.length == i)
                {
                    printUsage();
                }
                try
                {
                    maxMBs = Integer.parseInt(args[i]);
                }
                catch (NumberFormatException ignore)
                {
                    printUsage();
                }
            }
        }

        if (!root.isDirectory())
        {
            System.err.println(root.getAbsolutePath() + " is not a directory");
            printUsage();
        }
        if (!root.exists())
        {
            System.err.println(root.getAbsolutePath() + " does not exist");
            printUsage();
        }
        root = root.getCanonicalFile();
        System.out.println("Scanning " + root.getAbsolutePath() + " for files or dirs larger than " + maxMBs + " MB");
        printOutputFormat();
        long BYTES_IN_MB = 1024 * 1024L;
        FileData fileData = locateHugeFiles(root, maxMBs * BYTES_IN_MB);
        System.out.println("Total Size: " + ((fileData.getSize() / BYTES_IN_MB) + " MB"));

    }

    private static FileData locateHugeFiles(File dir, long maxSize)
    {
        assert dir != null && dir.isDirectory();
        FileData fileData = new FileData(dir);
        File[] files = dir.listFiles();
        if (files == null)
        {
            return fileData;
        }
        Arrays.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(File o1, File o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (File file : files)
        {
            if (file.canRead() && !isProblematicFile(file.toPath()))
            {
                if (file.isFile())
                {
                    FileData nestedFileData = new FileData(file);
                    nestedFileData.printIfOverLimit(maxSize);
                    fileData.addNestedFileSize(nestedFileData);
                }
                else if (file.isDirectory())
                {
                    FileData nestedFileData = locateHugeFiles(file, maxSize);
                    fileData.addNestedFileSize(nestedFileData);
                }
            }
        }
        fileData.printIfOverLimit(maxSize);
        return fileData;
    }

    private static final class FileData
    {
        File file;
        //size of this file or directory
        long size;
        //size of files or dirs within this dir whose huge size has already been reported to the user
        long reportedSize;
        long lastModified;

        FileData(File file)
        {
            this.file = file;
            if (file.isFile())
            {
                this.size = file.length();
                this.lastModified = file.lastModified();
            }
        }

        void addNestedFileSize(FileData fileData)
        {
            assert fileData != null;
            size += fileData.size;
            reportedSize += fileData.reportedSize;
            if (fileData.lastModified > lastModified)
            {
                lastModified = fileData.lastModified;
            }
        }

        void printIfOverLimit(long limit)
        {
            if (size - reportedSize >= limit)
            {
                System.out.printf("%6d MB %2d MO %s\n", (int) (size / (1024 * 1024L)),
                        (int) ((System.currentTimeMillis() - lastModified) / (1000L * 60 * 60 * 24 * 30)),
                        file.getAbsolutePath());
                reportedSize = size;
            }
        }

        public long getSize()
        {
            return size;
        }
    }

    static boolean isProblematicFile(Path path)
    {
        if (Files.isSymbolicLink(path))
        {
            return true;
        }
        boolean junction = false;
        try
        {
            junction = path.compareTo(path.toRealPath()) != 0;
        }
        catch (IOException ignore)
        {
        }
        return junction;
    }

    private static void printUsage()
    {
        System.out.println("java -jar huge.jar [-root rootdir] [-maxMBs <size in MB>] [-h]");
        System.out.println("Default root dir is the current directory");
        System.out.println("Default max size is 100 MB");
        printOutputFormat();
        System.exit(0);
    }

    private static void printOutputFormat()
    {
        System.out.println("Output Format: <size in MB> <time since last modification in months> <absolute file path>");
    }


}
