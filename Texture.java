import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * File: Texture.java
 * Created on 30.12.2023, 4:18:41
 *
 * @author LWJGL2
 */
public class Texture {

    public int[] pixelData;
    public final int width, height;

    public Texture(BufferedImage base) {
        BufferedImage image = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        {
            image.getGraphics().drawImage(base, 0, 0, null);
        }

        width = image.getWidth();
        height = image.getHeight();
        pixelData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    public int getPixel(float x, float y) {
        return getPixel((int) x, (int) y);
    }

    public int getPixel(int x, int y) {
        int index = x + y * width;
        if (index > pixelData.length - 1) {
            return 0;
        }
        if (index < 0) {
            return 0;
        }

        return pixelData[index];
    }
}
