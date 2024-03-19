package com.epam.deltix.data.connectors.reports;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    public static final String CONNECTION_REPORT = "Connection";
    public static final String VALIDATE_L2_REPORT = "Validate L2 Data";
    public static final String SUPPORTED_MODEL_REPORT = "Model";

    public static void generate(File outputFile, String title, Map<String, TestConnectorReports> testReports) {
        System.out.println("Printing file: " + outputFile.getAbsolutePath());
        List<String> tests = Arrays.asList(
            CONNECTION_REPORT, VALIDATE_L2_REPORT, SUPPORTED_MODEL_REPORT
        );

        try (OutputStreamWriter writer = new OutputStreamWriter(
            new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {

            writer.append(title);
            writer.append("\n\n");
            writer.append("| Exchange | ");
            for (String testName : tests) {
                writer.append(testName).append(" | ");
            }
            writer.append("\n| --- | --- | --- | --- |\n");
            for (TestConnectorReports testReport : testReports.values()) {
                writer.append("| ").append(prepareName(testReport.connector())).append(" | ");
                for (String testName : tests) {
                    TestReport report = testReport.reports().get(testName);
                    if (report != null) {
                        if (report.status() == TestStatus.INFO) {
                            writer.append(report.message())
                                .append(" | ");
                        } else {
                            if (report.status() == TestStatus.OK) {
                                writer.write("\u2705");
                            } else {
                                writer.write("\u274C");
                            }
                        }
                    }

                    writer.append(" | ");
                }

                writer.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String prepareName(String name) {
        return capitalize(name).replaceAll("-", " ");
    }

    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}
