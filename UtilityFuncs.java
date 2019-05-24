import java.util.*;
import java.awt.*;

public class UtilityFuncs {
    
    public static Random r = new Random();

    public static void drawKingIcon(Graphics g, int centerX, int centerY, int width, Color color) {        
        double p6_rad = Math.toRadians(25.0);
        double p2_rad = Math.PI - p6_rad;
        double p1_rad = Math.PI + p6_rad;
        double p7_rad = -p6_rad;
        int radius = (int) (0.4*width);
        int r1 = (int) (0.8*radius), r2 = (int) (0.4*radius), r3 = r1-r2/2;
        int jewel_diam = (int) (0.4*r1), jewel_radius = jewel_diam/2; 
        int p1X = (int) (centerX + r3*Math.cos(p1_rad));
        int p1Y = (int) (centerY - r3*Math.sin(p1_rad));
        int p2X = (int) (centerX + r1*Math.cos(p2_rad));
        int p2Y = (int) (centerY - r1*Math.sin(p2_rad));
        int p3X = (int) (centerX + r2*Math.cos(p2_rad));
        int p3Y = (int) (centerY - r2*Math.sin(p2_rad));        
        int p4X = centerX, p4Y = centerY - r1;
        int p5X = (int) (centerX + r2*Math.cos(p6_rad));
        int p5Y = (int) (centerY - r2*Math.sin(p6_rad));
        int p6X = (int) (centerX + r1*Math.cos(p6_rad));
        int p6Y = (int) (centerY - r1*Math.sin(p6_rad));
        int p7X = (int) (centerX + r3*Math.cos(p7_rad));
        int p7Y = (int) (centerY - r3*Math.sin(p7_rad));

        g.setColor(color);
        g.fillPolygon(new int[]{p1X, p2X, p3X, p4X, p5X, p6X, p7X},
                      new int[]{p1Y, p2Y, p3Y, p4Y, p5Y, p6Y, p7Y}, 7);
        g.fillOval(centerX - jewel_radius, centerY - jewel_radius, jewel_diam, jewel_diam);
        g.fillOval(p2X - jewel_radius, p2Y - jewel_radius, jewel_diam, jewel_diam);
        g.fillOval(p4X - jewel_radius, p4Y - jewel_radius, jewel_diam, jewel_diam);
        g.fillOval(p6X - jewel_radius, p6Y - jewel_radius, jewel_diam, jewel_diam);
        g.fillRect(p1X, p1Y + jewel_radius, p7X - p1X, jewel_diam);
    }
}
