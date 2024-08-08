package com.example.lims;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class LIMSDataProcessor {

    public static void main(String[] args) {
        try {
            // Step 1: Load Masses.csv
            Map<String, BigDecimal> masses = loadMasses("src/Masses.csv");
            System.out.println("Masses loaded: " + masses.size() + " entries found.");

            // Step 2: Process each Report.TXT file
            Path reportDir = Paths.get("src/data");
            System.out.println("Processing files in directory: " + reportDir.toAbsolutePath());

            Files.walk(reportDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("Report.TXT"))
                    .forEach(path -> {
                        System.out.println("Processing file: " + path.getFileName());
                        processReport(path, masses);
                    });

            System.out.println("Processing completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, BigDecimal> loadMasses(String fileName) throws IOException {
        Map<String, BigDecimal> masses = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            // Skip the header line
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String sampleName = parts[1].trim(); // Sample name is in the second column
                    BigDecimal mass = new BigDecimal(parts[2].trim()); // Mass is in the third column
                    masses.put(sampleName, mass);
                }
            }
        }
        return masses;
    }


//    private static void processReport(Path reportPath, Map<String, BigDecimal> masses) {
//        System.out.println("Processing file: " + reportPath.toString());
//
//        try (BufferedReader br = new BufferedReader(new FileReader(reportPath.toFile()))) {
//            String sampleName = "";
//            String instrument = "";
//            Date date = null;
//            BigDecimal dilution = BigDecimal.ONE;
//            BigDecimal sampleMass = null;
//            Map<String, BigDecimal> analytes = new HashMap<>();
//
//            String line;
//            while ((line = br.readLine()) != null) {
//                System.out.println("Reading line: " + line);
//
//                // Extract Sample Name
//                if (line.startsWith("Sample Name:")) {
//                    sampleName = line.split(":")[1].split(",")[0].trim();
//                    System.out.println("Sample Name found: " + sampleName);
//                }
//                // Extract Instrument and Date
//                else if (line.contains("LCMS-")) {
//                    instrument = line.split(" ")[0].trim();
//                    String dateString = line.substring(line.lastIndexOf(' ') + 1);
//                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
//                    date = sdf.parse(dateString);
//                    System.out.println("Instrument and Date found: " + instrument + ", " + date);
//                }
//                // Extract Dilution
//                else if (line.startsWith("Dilution:")) {
//                    dilution = new BigDecimal(line.split(":")[1].trim());
//                    System.out.println("Dilution found: " + dilution);
//                }
//                // Extract Analytes and their amounts
//                else if (line.matches("^\\s*\\d+\\.\\d+\\s+\\S+\\s+\\d+\\.\\d+\\s+\\d+\\.\\d+e?-?\\d*\\s+\\d+\\.\\d+\\s+\\S+")) {
//                    String[] parts = line.trim().split("\\s+");
//                    String analyteName = parts[5].trim();
//                    BigDecimal result = new BigDecimal(parts[4].trim());
//                    result = result.multiply(dilution); // Apply dilution factor
//                    analytes.put(analyteName, result);
//                    System.out.println("Analyte found: " + analyteName + " with result: " + result);
//                }
//            }
//
//            // Fetch sample mass if available
//            sampleMass = masses.getOrDefault(sampleName, BigDecimal.ZERO);
//
//            // Output results
//            if (!sampleName.isEmpty()) {
//                writeResults(sampleName, "SR", instrument, date, dilution, sampleMass, analytes);
//            } else {
//                System.out.println("No valid sample data found in the report file.");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private static void processReport(Path reportPath, Map<String, BigDecimal> masses) {
        System.out.println("Processing file: " + reportPath.toString());

        try (BufferedReader br = new BufferedReader(new FileReader(reportPath.toFile()))) {
            String sampleName = "";
            String instrument = "";
            Date date = null;
            BigDecimal dilution = BigDecimal.ONE;
            BigDecimal sampleMass = null;
            Map<String, BigDecimal> analytes = new HashMap<>();

            String line;
            boolean isDataSection = false; // To detect the start of data section
            while ((line = br.readLine()) != null) {
                System.out.println("Reading line: " + line);

                // Detect the start of the analyte section
                if (line.startsWith("Sorted By")) {
                    isDataSection = true;
                    continue;
                }

                // Extract Sample Name
                if (line.startsWith("Sample Name:")) {
                    sampleName = line.split(":")[1].split(",")[0].trim();
                    System.out.println("Sample Name found: " + sampleName);
                }
                // Extract Instrument and Date
                else if (line.contains("LCMS-")) {
                    instrument = line.split(" ")[0].trim();
                    String dateString = line.substring(line.lastIndexOf(' ') + 1);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                    date = sdf.parse(dateString);
                    System.out.println("Instrument and Date found: " + instrument + ", " + date);
                }
                // Extract Dilution
                else if (line.startsWith("Dilution:")) {
                    dilution = new BigDecimal(line.split(":")[1].trim());
                    System.out.println("Dilution found: " + dilution);
                }
                // Extract Analytes and their amounts
                else if (isDataSection && line.matches("^\\s*\\d+\\.\\d+\\s+\\S+\\s+\\d+\\.\\d+\\s+\\d+\\.\\d+e?-?\\d*\\s+\\d+\\.\\d+\\s+\\S+")) {
                    String[] parts = line.trim().split("\\s+");
                    String analyteName = parts[5].trim();
                    BigDecimal result = new BigDecimal(parts[4].trim());
                    result = result.multiply(dilution); // Apply dilution factor
                    analytes.put(analyteName, result);
                    System.out.println("Analyte found: " + analyteName + " with result: " + result);
                }
            }

            // Fetch sample mass if available
            sampleMass = masses.getOrDefault(sampleName, BigDecimal.ZERO);

            // Output results
            if (!sampleName.isEmpty()) {
                writeResults(sampleName, "SR", instrument, date, dilution, sampleMass, analytes);
            } else {
                System.out.println("No valid sample data found in the report file.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }








    private static void writeResults(String sampleName, String type, String instrument, Date date, BigDecimal dilution, BigDecimal sampleMass, Map<String, BigDecimal> analytes) throws IOException {
        String outputFilePath = "src/results/Results.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write(String.format("%s\t%s\t%s\t%s\t%s\t%s", sampleName, type, instrument, date, dilution, sampleMass));
            writer.newLine();
            for (Map.Entry<String, BigDecimal> entry : analytes.entrySet()) {
                writer.write(String.format("%s\t%.4f mg/mL", entry.getKey(), entry.getValue()));
                writer.newLine();
            }
        }
        System.out.println("Results written to: " + outputFilePath);
    }


}
