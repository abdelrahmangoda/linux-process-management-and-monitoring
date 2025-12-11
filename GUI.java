import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ProcessManagerProgram extends JFrame {

    public ProcessManagerProgram() {
        setTitle("Process Manager Program");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // ---------------- Buttons ----------------
        JButton showBtn = new JButton("Show Processes");
        showBtn.setBounds(20, 20, 200, 30);
        add(showBtn);

        JButton cpuBtn = new JButton("Start CPU Hog");
        cpuBtn.setBounds(20, 60, 200, 30);
        add(cpuBtn);

        JButton slowBtn = new JButton("Start Slow Process");
        slowBtn.setBounds(20, 100, 200, 30);
        add(slowBtn);

        JButton zombieBtn = new JButton("Start Zombie");
        zombieBtn.setBounds(20, 140, 200, 30);
        add(zombieBtn);

        JButton threadedBtn = new JButton("Start Threaded");
        threadedBtn.setBounds(20, 180, 200, 30);
        add(threadedBtn);

        JButton reniceBtn = new JButton("Renice Process");
        reniceBtn.setBounds(20, 220, 200, 30);
        add(reniceBtn);

        JButton killBtn = new JButton("Kill Process");
        killBtn.setBounds(20, 260, 200, 30);
        add(killBtn);

        // ---------------- Action Listeners ----------------
        showBtn.addActionListener(e -> showProcesses());
        cpuBtn.addActionListener(e -> startProcess("./cpu_hog"));
        slowBtn.addActionListener(e -> startProcess("./slow_process"));
        zombieBtn.addActionListener(e -> startProcess("./zombie"));
        threadedBtn.addActionListener(e -> startProcess("./threaded"));
        reniceBtn.addActionListener(e -> reniceProcess());
        killBtn.addActionListener(e -> killProcess());
    }

    // ---------------- Run process and capture output ----------------
    private String runCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    // ---------------- Actions ----------------
    private void showProcesses() {
        String output = runCommand("ps -eo pid,ppid,cmd,%cpu,%mem,stat --sort=-%cpu | head -15");
        JTextArea textArea = new JTextArea(output);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Processes", JOptionPane.INFORMATION_MESSAGE);
    }

    private void startProcess(String command) {
        runCommand(command + " &");
        JOptionPane.showMessageDialog(this, "Process started: " + command);
    }

    private void reniceProcess() {
        String pid = JOptionPane.showInputDialog(this, "Enter PID:");
        String nice = JOptionPane.showInputDialog(this, "Enter new nice value:");
        try {
            runCommand("sudo renice " + nice + " -p " + pid);
            JOptionPane.showMessageDialog(this, "Reniced PID " + pid + " to " + nice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void killProcess() {
        String pid = JOptionPane.showInputDialog(this, "Enter PID:");
        String[] options = {"SIGTERM", "SIGKILL"};
        int choice = JOptionPane.showOptionDialog(this, "Choose signal", "Kill Process",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        String signal = choice == 0 ? "-TERM" : "-KILL";
        runCommand("kill " + signal + " " + pid);
        JOptionPane.showMessageDialog(this, "Signal " + signal + " sent to PID " + pid);
    }

    // ---------------- Main ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProcessManagerProgram().setVisible(true));
    }
}
