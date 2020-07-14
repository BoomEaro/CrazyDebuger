package ru.boomearo.crazydebuger.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Ziping {

	
	//ВСе это не мое
	public static void zipDir(File source_dir, File zip_file) throws Exception {
	
	    // Cоздание объекта ZipOutputStream из FileOutputStream
	    FileOutputStream fout = new FileOutputStream(zip_file);
	    ZipOutputStream zout = new ZipOutputStream(fout);
	        	
	    // Создание объекта File object архивируемой директории
	               
	    addDirectory(zout, source_dir);
	               
	    // Закрываем ZipOutputStream
	    zout.close();
	               
	}
	
	public static void zipFile(File source, File zip) {
        try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zip));
                FileInputStream fis= new FileInputStream(source);) {
        	
        
            ZipEntry entry1 = new ZipEntry(source.getName());
            zout.putNextEntry(entry1);
            // считываем содержимое файла в массив byte
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            // добавляем содержимое к архиву
            zout.write(buffer);
            // закрываем текущую запись для новой записи
            zout.closeEntry();
        }
        catch (Exception ex){
            ex.printStackTrace();
        } 
	}
	
	private static void addDirectory(ZipOutputStream zout, File fileSource) throws Exception {
	
	    File[] files = fileSource.listFiles();
	    //System.out.println("Добавление директории <" + fileSource.getName() + ">");
	    for(int i = 0; i < files.length; i++) {
	        // Если file является директорией, то рекурсивно вызываем 
	        // метод addDirectory
	        if(files[i].isDirectory()) {
	            addDirectory(zout, files[i]);
	            continue;
	        }
	       // System.out.println("Добавление файла <" + files[i].getName() + ">");

	        FileInputStream fis = new FileInputStream(files[i]);
	       		
	        zout.putNextEntry(new ZipEntry(files[i].getName()));

	        byte[] buffer = new byte[4048];
	        int length;
	        while((length = fis.read(buffer)) > 0)
	            zout.write(buffer, 0, length);
		    // Закрываем ZipOutputStream и InputStream
	        zout.closeEntry();
	        fis.close();
	    }
	}
}
