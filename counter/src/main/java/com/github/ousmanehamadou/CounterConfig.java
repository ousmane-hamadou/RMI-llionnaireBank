package com.github.ousmanehamadou;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "RMI-llionaireBank-Client",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Client-side configuration to connect to the RMI-llionaireBank server."
)

public class CounterConfig implements Runnable {

    @Option(names = {"-p", "--port"},
            description = "The target port where the RMI registry is running. Default: ${DEFAULT-VALUE}",
            defaultValue = "1099")
    private int port;

    @Option(names = {"-s", "--server-ip"},
            description = "The remote server IP address or hostname. Default: ${DEFAULT-VALUE}",
            defaultValue = "127.0.0.1")
    private String serverIp;

    @Option(names = {"-n", "--name"},
            description = "The remote service name to look up in the RMI registry.",
            required = true)
    private String serviceName;

    @Override
    public void run() {
        System.out.println("--- RMI-llionaireBank Client Connecting ---");
        System.out.println("Target Service: " + serviceName);
        System.out.println("Remote Address: " + serverIp + ":" + port);
    }

    // Getters
    public int getPort() { return port; }
    public String getServerIp() { return serverIp; }
    public String getServiceName() { return serviceName; }
}

