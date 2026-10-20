import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

// Scales a square texture down with area averaging (no Python image library needed).
// Usage: java tools/Resize.java <in.png> <out.png> <size>
public class Resize {
    public static void main(String[] args) throws Exception {
        BufferedImage in = ImageIO.read(new File(args[0]));
        int size = Integer.parseInt(args[2]);
        Image scaled = in.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        ImageIO.write(out, "png", new File(args[1]));
    }
}
