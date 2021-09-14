package ru.boomearo.crazydebuger.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zipping {

    //ВСе это не мое
    public static void zipDir(File source_dir, File zip_file, boolean recursion) {

        // Создание объекта ZipOutputStream из FileOutputStream
        try (ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(zip_file))) {
            // Создание объекта File object архивируемой директории

            addDirectory(zOut, source_dir, recursion);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void zipFile(File source, File zip) {
        try (ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(zip));
             FileInputStream fis = new FileInputStream(source);) {


            ZipEntry entry1 = new ZipEntry(source.getName());
            zOut.putNextEntry(entry1);
            // считываем содержимое файла в массив byte
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            // добавляем содержимое к архиву
            zOut.write(buffer);
            // закрываем текущую запись для новой записи
            zOut.closeEntry();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void addDirectory(ZipOutputStream zOut, File fileSource, boolean recursion) {

        File[] files = fileSource.listFiles();
        if (files == null) {
            return;
        }

        //System.out.println("Добавление директории <" + fileSource.getName() + ">");
        for (File file : files) {
            // Если file является директорией, то рекурсивно вызываем
            // метод addDirectory

            //Если передан аргумент рекурсии, значит ищет внутри папки
            if (file.isDirectory()) {
                if (recursion) {
                    addDirectory(zOut, file, recursion);
                }
                continue;
            }
            // System.out.println("Добавление файла <" + files[i].getName() + ">");

            try (FileInputStream fis = new FileInputStream(file)) {

                zOut.putNextEntry(new ZipEntry(file.getName()));

                byte[] buffer = new byte[4048];
                int length;
                while ((length = fis.read(buffer)) > 0)
                    zOut.write(buffer, 0, length);
                // Закрываем ZipOutputStream и InputStream
                zOut.closeEntry();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
