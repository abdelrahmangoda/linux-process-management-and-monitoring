// GUI.java  (real PID popup + instant search + live Gantt)
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class GUI extends JFrame {

    private static final Color BG_DARK = new Color(31, 40, 51);
    private static final Color ACCENT  = new Color(102, 252, 241);
    private static final Color CANVAS  = new Color(41, 54, 68);
    private static final Color TEXT    = new Color(197, 198, 199);
    private static final Color RED     = new Color(255, 69, 0);
    private static final Color YELLOW  = new Color(255, 215, 0);
    private static final Color GREEN   = new Color(44, 191, 100);
    private static final Color GRAY    = new Color(100, 100, 100);

    private JTable table;
    private DefaultTableModel model;
    private List<ProcessData> procs = new ArrayList<>();
    private JLabel cpuLbl, ramLbl, cntLbl;
    private JTextArea logArea;
    private JTextField searchField;
    private GanttChartPanel ganttPanel;
    private javax.swing.Timer refresher;

    private static class ProcessData {
        int pid, nice, prio; String user, state, cmd; double cpu; long born;
        ProcessData(int p, String u, int n, int pr, String s, double c, String cm){
            pid = p; user = u; nice = n; prio = pr; state = s; cpu = c; cmd = cm; born = System.currentTimeMillis();
        }
    }

    public static void main(String[] args){ SwingUtilities.invokeLater(GUI::new); }

    public GUI(){
        super("Linux Process Manager (real PID + instant search)");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        ensureHelpers();

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        p.add(createTopBar(), BorderLayout.NORTH);
        p.add(createCenter(), BorderLayout.CENTER);
        p.add(createLog(), BorderLayout.SOUTH);

        add(p);
        startRefresh();
        setVisible(true);
    }

    private JPanel createTopBar(){
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        bar.setBackground(BG_DARK);

        cpuLbl = label("CPU --");
        ramLbl = label("RAM --");
        cntLbl = label("Procs --");
        bar.add(cpuLbl); bar.add(ramLbl); bar.add(Box.createHorizontalStrut(30)); bar.add(cntLbl);

        bar.add(Box.createHorizontalGlue());

        // search field
        searchField = new JTextField(12);
        searchField.setToolTipText("Search PID / command / user");
        searchField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        searchField.addActionListener(e -> applySearch());
        bar.add(label("Search:"));
        bar.add(searchField);

        JButton searchBtn = btn("Go", e -> applySearch());
        bar.add(searchBtn);

        // spawn buttons
        bar.add(btn("CPU Hog", e -> spawn("./cpu_hog", "High CPU load")));
        bar.add(btn("Slow",    e -> spawn("./slow_process", "Sleep-heavy")));
        bar.add(btn("Zombie",  e -> spawn("./zombie", "Zombie creator")));
        bar.add(btn("Threads", e -> spawn("./threaded", "Multi-thread CPU")));
        return bar;
    }

    private void applySearch(){
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()){ table.clearSelection(); return; }
        for (int i = 0; i < model.getRowCount(); i++){
            int pid   = (int)    model.getValueAt(i, 0);
            String user = model.getValueAt(i, 1).toString().toLowerCase();
            String cmd  = model.getValueAt(i, 6).toString().toLowerCase();
            if (String.valueOf(pid).equals(q) || user.contains(q) || cmd.contains(q)){
                table.getSelectionModel().setSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
        table.clearSelection();
    }

    private JButton btn(String text, java.awt.event.ActionListener a){
        JButton b = new JButton(text);
        b.setForeground(BG_DARK);
        b.setFont(new Font("Monospaced", Font.BOLD, 12));
        b.setUI(new BasicButtonUI(){
            public void paint(Graphics g, JComponent c){
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
                super.paint(g, c);
            }
        });
        b.addActionListener(a);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private JSplitPane createCenter(){
        model = new DefaultTableModel(new Object[]{"PID","USER","NICE","PR","STATE","%CPU","COMMAND"}, 0){
            public boolean isCellEditable(int r, int c){ return false; }
            public Class<?> getColumnClass(int c){ return c==0||c==2||c==3 ? Integer.class : c==5 ? Double.class : String.class; }
        };
        table = new JTable(model);
        table.setRowHeight(26);
        table.setBackground(BG_DARK);
        table.setForeground(TEXT);
        table.setSelectionBackground(ACCENT.darker());
        table.getTableHeader().setBackground(ACCENT);
        table.getTableHeader().setForeground(BG_DARK);
        table.setAutoCreateRowSorter(true);
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if (e.getClickCount() == 1 && table.getSelectedRow() != -1){
                    int m = table.convertRowIndexToModel(table.getSelectedRow());
                    int pid = (int) model.getValueAt(m, 0);
                    ProcessData d = procs.stream().filter(p -> p.pid == pid).findFirst().orElse(null);
                    if (d != null) showKillReniceDialog(d);
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(ACCENT));
        ganttPanel = new GanttChartPanel();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp, ganttPanel);
        split.setResizeWeight(0.6);
        split.setDividerLocation(700);
        split.setBorder(null);
        return split;
    }

    private JPanel createLog(){
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ACCENT), "Terminal Output",
                0, 0, new Font("Monospaced", Font.BOLD, 12), ACCENT));
        logArea = new JTextArea(4, 1);
        logArea.setEditable(false);
        logArea.setBackground(new Color(25, 30, 36));
        logArea.setForeground(TEXT);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }

    private JLabel label(String t){ JLabel l = new JLabel(t); l.setForeground(TEXT); return l; }

    // ------------------------------ KILL / RENICE DIALOG ------------------------------
    private void showKillReniceDialog(ProcessData d){
        JDialog dlg = new JDialog(this, "PID " + d.pid + "  |  " + d.cmd, true);
        dlg.setSize(380, 200);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER));
        info.setBackground(BG_DARK);
        info.add(label("PID: " + d.pid + "   Nice: " + d.nice + "   CPU: " + String.format("%.1f%%", d.cpu)));

        JPanel kill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        kill.setBackground(BG_DARK);
        JButton sigterm = new JButton("Kill SIGTERM");
        JButton sigkill = new JButton("Kill SIGKILL");
        sigterm.addActionListener(e -> { kill(d, "TERM"); dlg.dispose(); });
        sigkill.addActionListener(e -> { kill(d, "KILL"); dlg.dispose(); });
        kill.add(sigterm); kill.add(sigkill);

        JPanel ren = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        ren.setBackground(BG_DARK);
        JTextField nf = new JTextField(String.valueOf(d.nice), 4);
        ren.add(new JLabel("New nice:"));
        ren.add(nf);
        JButton renBtn = new JButton("RENICE");
        renBtn.addActionListener(e -> {
            try {
                int n = Integer.parseInt(nf.getText().trim());
                renice(d, n);
                dlg.dispose();
            } catch (NumberFormatException ignore) {}
        });
        ren.add(renBtn);

        p.add(info, BorderLayout.NORTH);
        p.add(kill, BorderLayout.CENTER);
        p.add(ren, BorderLayout.SOUTH);

        dlg.add(p);
        dlg.setVisible(true);
    }

    // ------------------------------ SPAWN WITH NICE + PID POPUP + INSTANT SEARCH ------------------------------
    private void spawn(String exe, String desc){
        String in = JOptionPane.showInputDialog(this, desc + "\nNice value (-20 … 19):", 0);
        int nice = 0;
        try { if (in != null && !in.trim().isEmpty()) nice = Integer.parseInt(in.trim()); }
        catch (NumberFormatException ignore) {}
        if (nice < -20) nice = -20; if (nice > 19) nice = 19;

        try{
            ProcessBuilder pb = new ProcessBuilder("bash", "-c",
                    "nice -n " + nice + " " + exe + " & echo $!");
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))){
                String pidStr = br.readLine();          // PID printed by bash
                if (pidStr != null){
                    int pid = Integer.parseInt(pidStr.trim());
                    log("Spawned " + exe + " with nice " + nice + "  →  PID " + pid);
                    // small popup so you see it immediately
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this,
                                    "Started " + exe + "\nPID: " + pid,
                                    "New Process",
                                    JOptionPane.INFORMATION_MESSAGE)
                    );

                    // 🔍 INSTANTLY refresh & jump to that PID
                    startRefresh();   // fetch now
                    SwingUtilities.invokeLater(() -> {
                        for (int i = 0; i < model.getRowCount(); i++){
                            if ((int)model.getValueAt(i,0) == pid){
                                table.getSelectionModel().setSelectionInterval(i,i);
                                table.scrollRectToVisible(table.getCellRect(i,0,true));
                                break;
                            }
                        }
                    });
                }
            }
        }catch(Exception ex){ log("Spawn failed: " + ex.getMessage()); }
    }

    // ------------------------------ KILL / RENICE / RUN ------------------------------
    private void kill(ProcessData d, String sig){
        run("kill -" + sig + " " + d.pid);
        log("Sent SIG" + sig + " to PID " + d.pid);
    }

    private void renice(ProcessData d, int n){
        run("sudo renice " + n + " -p " + d.pid);
        log("Renice PID " + d.pid + " → " + n);
    }

    private void run(String cmd){
        try { new ProcessBuilder("bash", "-c", cmd).start(); }
        catch (IOException ex) { log("Run failed: " + ex.getMessage()); }
    }

    // ------------------------------ LIVE DATA + GANTT REPAINT ------------------------------
    private void startRefresh(){
        refresher = new javax.swing.Timer(1200, e -> {
            fetchProcs();
            updateHealth();
            refreshTable();
            SwingUtilities.invokeLater(() -> { if (ganttPanel != null) ganttPanel.repaint(); });
        });
        refresher.setInitialDelay(0);
        refresher.start();
    }

    private void fetchProcs(){
        List<ProcessData> tmp = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ProcessBuilder("bash","-c","ps -eo pid,user,ni,pri,stat,%cpu,cmd --sort=-%cpu").start().getInputStream()))){
            String line; boolean header=true;
            while ((line = br.readLine()) != null){
                if (header){ header=false; continue; }
                Scanner sc = new Scanner(line);
                if (!sc.hasNextInt()) continue;
                int pid = sc.nextInt(); String user = sc.next();
                int nice = sc.nextInt(); int pr = sc.nextInt();
                String state = sc.next(); double cpu = sc.nextDouble();
                String cmd = line.substring(line.indexOf(String.valueOf(cpu)) + String.valueOf(cpu).length()).trim();
                tmp.add(new ProcessData(pid,user,nice,pr,state,cpu,cmd));
            }
        }catch(Exception ignore){}
        procs = tmp;
    }

    private void updateHealth(){
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ProcessBuilder("bash","-c","top -bn1 | head -5").start().getInputStream()))){
            String line; double cpu=0, total=1, used=1; int cnt=0;
            while ((line = br.readLine()) != null){
                if (line.startsWith("%Cpu")){
                    String[] x=line.split(","); for (String s:x) if (s.endsWith("id")) cpu=100-Double.parseDouble(s.trim().split("\\s+")[0]);
                }
                if (line.contains("Tasks:")) cnt = Integer.parseInt(line.split("\\s+")[1]);
                if (line.contains("MiB Mem :")){
                    String[] x=line.split(","); total=Double.parseDouble(x[0].split(":")[1].trim().split("\\s+")[0]);
                    used  =Double.parseDouble(x[1].trim().split("\\s+")[0]);
                }
            }
            cpuLbl.setText(String.format("CPU: %.1f%%", cpu));
            ramLbl.setText(String.format("RAM: %.1f/%.1f MiB", used, total));
            cntLbl.setText("Procs: " + cnt);
        }catch(Exception ignore){}
    }

    private void refreshTable(){
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            for (ProcessData d : procs)
                model.addRow(new Object[]{d.pid, d.user, d.nice, d.prio, d.state, d.cpu, d.cmd});
        });
    }

    // ------------------------------ GANTT CHART ------------------------------
    private class GanttChartPanel extends JPanel {
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CANVAS);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (procs.isEmpty()){ g2.setColor(TEXT); g2.drawString("No processes", getWidth()/2-50, getHeight()/2); return; }
            int y=10, h=22, gap=6, left=210, pad=10;
            for (ProcessData d : procs){
                if (y+h+gap>getHeight()) break;
                int w = (int)((getWidth()-left-pad*2)*(d.cpu/100.0));
                Color c = d.state.contains("Z") ? GRAY : d.cpu>80?RED:d.cpu>40?YELLOW:GREEN;
                g2.setColor(c); g2.fillRect(left, y, w, h);
                g2.setColor(TEXT);
                g2.drawString(String.format("[%d] %s", d.pid, d.cmd.length()>20?d.cmd.substring(0,17)+"...":d.cmd), pad, y+h/2+5);
                g2.drawString(String.format("%.1f%%", d.cpu), left+w+5, y+h/2+5);
                y+=h+gap;
            }
        }
    }

    private void ensureHelpers(){
        Map<String,String> src = Map.of(
            "cpu_hog",    "#include <unistd.h>\nint main(){while(1); return 0;}",
            "slow_process","#include <unistd.h>\nint main(){sleep(1000); return 0;}",
            "zombie",      "#include <unistd.h>\nint main(){if(fork()==0)exit(0); sleep(1000); return 0;}",
            "threaded",    "#include <pthread.h>\n#include <unistd.h>\nvoid*f(void*_){while(1);return NULL;}\nint main(){pthread_t t1,t2;pthread_create(&t1,NULL,f,NULL);pthread_create(&t2,NULL,f,NULL);pthread_join(t1,NULL);pthread_join(t2,NULL);return 0;}");
        for (var e : src.entrySet()){
            Path exe = Paths.get(e.getKey());
            if (Files.exists(exe)) continue;
            try{
                Path c = Files.writeString(Paths.get(e.getKey()+".c"), e.getValue());
                new ProcessBuilder("gcc", c.toString(), "-o", e.getKey(), "-pthread").inheritIO().start().waitFor();
                Files.deleteIfExists(c);
                Runtime.getRuntime().exec("chmod +x " + e.getKey()).waitFor();
            }catch (Exception ex){ log("Build helper failed: " + ex.getMessage()); }
        }
    }

    private void log(String msg){
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.util.Date().toString().substring(11,19) + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
