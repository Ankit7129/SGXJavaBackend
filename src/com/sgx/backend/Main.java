package com.sgx.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class Main {

    // Path to your SSH private key (update as needed)
    private static final String SSH_KEY_PATH = System.getProperty("user.home") + "\\Downloads\\sgx-7_key.pem";

    // Your Azure VM details (update as needed)
    private static final String VM_IP = "20.115.92.71";
    private static final String VM_USERNAME = "ankit";

    public static void main(String[] args) {
        System.out.println("Generating vectors and sending to Azure VM...");

        String targetDir = "/home/ankit/data";
        String file1 = "vector1.txt";
        String file2 = "vector2.txt";

        // Generate two random vectors as space-separated strings
        String vector1 = generateVector(100);
        String vector2 = generateVector(100);

        try {
            // Escape any single quotes for shell safe command
            String escapedVector1 = vector1.replace("'", "'\"'\"'");
            String escapedVector2 = vector2.replace("'", "'\"'\"'");

            // Build the remote command to create directory and write vectors to files
            String remoteCommand = String.format(
                "mkdir -p %s && echo '%s' > %s/%s && echo '%s' > %s/%s",
                targetDir,
                escapedVector1,
                targetDir,
                file1,
                escapedVector2,
                targetDir,
                file2
            );

            // Execute SSH command to upload files
            testSSHCommand(remoteCommand);

            System.out.println("Vector files uploaded to VM:");
            System.out.println("   • " + targetDir + "/" + file1);
            System.out.println("   • " + targetDir + "/" + file2);
        } catch (Exception e) {
            System.err.println("Failed to send vector files:");
            e.printStackTrace();
        }
    }

    // Generates a space-separated vector of random integers (0-9)
    private static String generateVector(int size) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(random.nextInt(10));  // numbers from 0 to 9
            if (i < size - 1) {
                sb.append(" ");  // space separator
            }
        }
        return sb.toString();
    }

    // Runs a remote SSH command on the Azure VM
    public static void testSSHCommand(String remoteCommand) throws IOException, InterruptedException {
        System.out.println("Running remote command: " + remoteCommand);

        String sshCommand = String.format(
            "ssh -o StrictHostKeyChecking=no -i \"%s\" %s@%s \"%s\"",
            SSH_KEY_PATH,
            VM_USERNAME,
            VM_IP,
            remoteCommand
        );

        System.out.println("Executing: " + sshCommand);

        Process process = Runtime.getRuntime().exec(sshCommand);

        // Read standard output from remote command
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("VM Response: " + line);
            }
        }

        // Read error output from remote command
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.err.println("VM Error: " + errorLine);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("SSH command failed with exit code: " + exitCode);
        }
    }
}
