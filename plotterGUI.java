package plotter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

import com.fazecast.jSerialComm.*;



public class PlotterApp {

    static int pointThreshold = 6;
    static ArrayList<Stroke> strokes = new ArrayList<Stroke>();

    static Stroke currentStroke = new Stroke();

    static int mx = 0;
    static int my = 0;
    public static void main(String[] args) throws InterruptedException {
        SwingUtilities.invokeLater(() -> new PlotterApp().createAndShowGUI());



    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Dual Arm Plotter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        //Drawing Panel
        DrawingPanel drawingPanel = new DrawingPanel();
        frame.add(drawingPanel, BorderLayout.CENTER);

        //Button Panel (bottom)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JButton clearButton = new JButton("Clear");
        JButton sendButton = new JButton("Send to Arduino");
        JButton undoButton = new JButton("Undo");
        JButton printButton = new JButton("Print points");


        //Add buttons
        buttonPanel.add(clearButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(sendButton);
        buttonPanel.add(printButton);


        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.pack();

        //Button Actions
        clearButton.addActionListener(e -> {
            
            strokes.clear();
            currentStroke.points.clear();
            drawingPanel.repaint();
        });

        undoButton.addActionListener(e -> {
            
            currentStroke.points.clear();
            if (!strokes.isEmpty()) strokes.remove(strokes.size() - 1);
            drawingPanel.repaint();
        });

        sendButton.addActionListener(e -> {
           
            System.out.println("Send clicked");

            SerialPort port = SerialPort.getCommPort("COM5"); 

            port.setBaudRate(115200);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);

            if (!port.openPort()) {
                System.out.println("Failed to open port");
                return;
            }

            System.out.println("Connected!");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            for (Stroke s : strokes) {
                ArrayList<Float> xPoints = new ArrayList<Float>();
                ArrayList<Float> yPoints = new ArrayList<Float>();

                for (Point p : s.points) {
                    xPoints.add(p.scaledX);
                    yPoints.add(p.scaledY);

                }

                xPoints.add(1, 1f);
                yPoints.add(1, 1f);

                xPoints.add(0f);
                yPoints.add(0f);

                for (int i = 0; i < xPoints.size(); i++) {
                    sendPoint(port, xPoints.get(i), yPoints.get(i));
                    String response = null;
                    try {
                        response = readLine(port);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (!response.equals("OK")) {
                        System.out.println("unexpected response: " + response);
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }


            }

            port.closePort();
        });

        printButton.addActionListener(e -> {
            // TODO: print points
            for (Stroke s : strokes) {
                System.out.print(s.points.get(0).scaledX + ", 1, ");

                for (int i = 1; i < s.points.size() - 1; i++) {
                    System.out.print(s.points.get(i).scaledX + ", ");
                }
                System.out.print("0, ");
            }
            System.out.println();
            for (Stroke s : strokes) {
                System.out.print(s.points.get(0).scaledY + ", 1, ");

                for (int i = 1; i < s.points.size() - 1; i++) {
                    System.out.print(s.points.get(i).scaledY + ", ");
                }
                System.out.print("0, ");
            }



        });



        frame.setVisible(true);
    }

  
    //Drawing Panel Class

    static class DrawingPanel extends JPanel {

        public DrawingPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(600, 400));
            //Mouse listeners
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    //start stroke
                    currentStroke = new Stroke();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                   
                    strokes.add(currentStroke);
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    
                    mx = e.getX();
                    my = e.getY();
                    if (currentStroke.points.isEmpty()) currentStroke.addPoint(new Point(mx, my));
                    else {
                        Point lastPoint = currentStroke.points.get(currentStroke.points.size() - 1);
                        int dx = Math.abs(lastPoint.x - mx);
                        int dy = Math.abs(lastPoint.y - my);

                        if (dx > pointThreshold || dy > pointThreshold) currentStroke.addPoint(new Point(mx, my)); //make sure points aren't too close
                    }




                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            // Enable smooth rendering
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            
            g2.setColor(Color.GRAY);
            g2.drawString("Draw here", 20, 20);
            g2.drawString("x: " + mx, 20, 40);
            g2.drawString("y: " + my, 20, 60);

            g2.setColor(Color.BLACK);

//            for (Point p : currentStroke.points) {
//                g2.fillOval(p.x, p.y, 5, 5);
//            }

            for (int i = 1; i < currentStroke.points.size(); i++) {
                Point p1 = currentStroke.points.get(i - 1);
                Point p2 = currentStroke.points.get(i);

                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                g2.fillOval(p1.x - 4, p1.y - 4, 8, 8);
            }



            for (int i = 0; i < strokes.size(); i++) {
                for (int j = 1; j < strokes.get(i).points.size(); j++) {
                    Point p1 = strokes.get(i).points.get(j - 1);
                    Point p2 = strokes.get(i).points.get(j);

                    g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    g2.fillOval(p1.x - 4, p1.y - 4, 8, 8);

                }
                if (strokes.get(i).points.size() > 1) {
                    Point lastPoint = strokes.get(i).points.get(strokes.get(i).points.size() - 1);
                    g2.fillOval(lastPoint.x - 4, lastPoint.y - 4, 8, 8);
                }
            }

            g2.setColor(new Color(255, 0, 0, 128));
            g2.fillOval(160, 270, 272, 272);
        }
    }

    static void sendPoint(SerialPort port, float x, float y) {
        String cmd = String.format("X%.2f Y%.2f\n", x, y);
        System.out.println(cmd);
        port.writeBytes(cmd.getBytes(), cmd.length());
    }

    static String readLine(SerialPort port) throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            while (port.bytesAvailable() == 0) {
                Thread.sleep(1);
            }

            byte[] buffer = new byte[1];
            port.readBytes(buffer, 1);

            if (buffer[0] == '\n') break;
            sb.append((char) buffer[0]);
        }
        return sb.toString().trim();
    }
}


public class Point {
    public int x, y;
    public float scaledX, scaledY;

    Point(int x, int y) {
        this.x = x;
        this.y = y;

        this.scaledX = (this.x - 300)/2.85714286f;

        this.scaledY = (400 - this.y)/2.85714286f;
    }
}



public class Stroke {
    ArrayList<Point> points;

    Stroke(ArrayList<Point> ps) {
        this.points = new ArrayList<Point>();
        for (Point p : ps) {
            this.points.add(p);
        }
    }

    Stroke() {
        this.points = new ArrayList<Point>();
    }

    public void addPoint(Point p) {
        this.points.add(p);
    }
 }
