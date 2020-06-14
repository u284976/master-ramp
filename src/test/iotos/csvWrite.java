package test.iotos;

import java.io.File;
import java.io.FileWriter;

import com.opencsv.CSVWriter;

public class csvWrite{

    public static void main(String[] args){
        String outputFile = "test_output.csv";
        boolean alreadyExists = new File(outputFile).exists();
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile, false), ',');
            String[] entries1 = "data1,data2,data3". split(","); 
            String[] entries2 = {"data1","data2","data3"}; 
            writer.writeNext(entries1);
            writer.writeNext(entries2);  
            writer.close();
        } catch (Exception e) {
        }
    }
}