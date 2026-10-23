package dev.jvmmcp;

import dev.jvmmcp.core.attach.JvmAttachService;
import dev.jvmmcp.core.model.JvmProcess;
import dev.jvmmcp.core.util.SimpleJson;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "list",
    aliases = {"ps", "ls"},
    description = "Lists all running Java Virtual Machines on the host",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output in JSON format")
    boolean json;

    private final JvmAttachService service;

    public ListCommand() {
        this.service = new JvmAttachService();
    }

    public ListCommand(JvmAttachService service) {
        this.service = service;
    }

    @Override
    public Integer call() {
        List<JvmProcess> jvms = service.listJvms();

        if (json) {
            System.out.println(SimpleJson.toJson(jvms));
            return 0;
        }

        if (jvms.isEmpty()) {
            System.out.println("No running JVM processes discovered.");
            return 0;
        }

        System.out.printf("%-10s %-16s %-30s %s%n", "PID", "FRAMEWORK", "MAIN CLASS", "DISPLAY NAME");
        System.out.println("-".repeat(80));

        for (JvmProcess jvm : jvms) {
            String mainClass = jvm.mainClass();
            if (mainClass.length() > 28) {
                mainClass = mainClass.substring(0, 25) + "...";
            }
            System.out.printf(
                "%-10d %-16s %-30s %s%n",
                jvm.pid(),
                jvm.framework().getDisplayName(),
                mainClass,
                jvm.displayName()
            );
        }
        return 0;
    }
}
