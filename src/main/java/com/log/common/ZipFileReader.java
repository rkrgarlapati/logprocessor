package com.log.common;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

//GZIP2 and TAR file decompressing

public class ZipFileReader {

    private static ZipFileReader instance;
    private File unzippedfolder;

    private ZipFileReader() {
    }

    public static ZipFileReader getInstance() {
        if (instance == null) {
            instance = new ZipFileReader();
        }

        return instance;
    }

    public static void main(String[] args) {
        ZipFileReader.getInstance().zipParser("/Users/ravi/511-3.zip");
    }

    public List<File> zipParser(String fpath) {

        List<File> files = null;

        try {
            CleanUpFiles cleanUpFiles = CleanUpFiles.getInstance();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String dtime = LocalDateTime.now().format(formatter);

            System.out.println("Zip decompressing started...."+new Date());
            Path path = Paths.get(fpath);
            InputStream fin = Files.newInputStream(path);
            BufferedInputStream in = new BufferedInputStream(fin);

            String folderN = path.getParent() + "/" + dtime;
            String outZip = folderN + ".zip";

            cleanUpFiles.setFolderName(folderN);
            cleanUpFiles.setZipName(outZip);

            OutputStream out = Files.newOutputStream(Paths.get(outZip));
            BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
            final byte[] buffer = new byte[20000];
            int n = 0;
            while (-1 != (n = bzIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
            out.close();
            bzIn.close();

            File input = new File(outZip);
            File output = new File(folderN);

            setUnzippedfolder(output);

            files = unTar(input, output);

            System.out.println("Zip decompressing completed...."+new Date());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return files;
    }


    private static List<File> unTar(final File inputFile, final File outputDir) throws FileNotFoundException, IOException, ArchiveException {

        final InputStream is = new FileInputStream(inputFile);
        final TarArchiveInputStream debInputStream = (TarArchiveInputStream)
                new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry;

        outputDir.mkdirs();
        final List<File> untaredFiles = new LinkedList<File>();
        while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {

            final File outputFile = new File(outputDir, entry.getName());
            if (entry.isDirectory()) {
                System.out.println(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
                if (!outputFile.exists()) {
                    System.out.println(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
                    if (!outputFile.mkdirs()) {
                        throw new IllegalStateException(String.format("Couldn't create directory %s.", outputFile.getAbsolutePath()));
                    }
                }
            } else {
                new File(outputFile.getParent()).mkdirs();
                //System.out.println(String.format("Creating output file %s", outputFile.getAbsolutePath()));
                final OutputStream outputFileStream = new FileOutputStream(outputFile);
                IOUtils.copy(debInputStream, outputFileStream);
                outputFileStream.close();
            }
            untaredFiles.add(outputFile);
        }

        //System.out.println("Size ::" + untaredFiles.size());

        return untaredFiles;
    }

    public File getUnzippedfolder() {
        return unzippedfolder;
    }

    public void setUnzippedfolder(File unzippedfolder) {
        this.unzippedfolder = unzippedfolder;
    }
}
