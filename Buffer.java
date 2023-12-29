import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * File: Buffer.java
 * Created on 30.12.2023, 2:04:34
 *
 * @author LWJGL2
 */
public class Buffer {

    private BufferedImage image;

    public int[] pixelData;
    public float[] depthBuffer;
    public int width, height;

    public Matrix4f normalMatrix, modelMatrix, viewMatrix, projectionMatrix;
    public int fps;

    public Buffer(int width, int height) {
        create(width, height);
    }

    public void clear(Color color) {
        Arrays.fill(pixelData, color.getRGB());
        Arrays.fill(depthBuffer, 0);
    }

    public void drawPixel(int x, int y, Color color) {
        drawPixel(x, y, color.getRGB());
    }

    public void drawPixel(int x, int y, int color) {
        int index = x + y * width;
        if (index > pixelData.length - 1) {
            return;
        }
        if (index < 0) {
            return;
        }

        pixelData[index] = color;
    }

    public void create(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixelData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        depthBuffer = new float[width * height];
    }

    public void draw(Graphics g, int width, int height) {

        boolean debug = false;
        if (debug) {
            try {
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {

                        float raw = depthBuffer[i + j * width];
                        if (raw > 255) {
                            raw = 255;
                        }
                        drawPixel(i, j, new Color((int) raw, (int) raw, (int) raw));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        g.drawImage(image, 0, 0, width, height, null);
    }

    public void drawLine(Color color, int x1, int y1, int x2, int y2) {
        final int deltaX = Math.abs(x2 - x1);
        final int deltaY = Math.abs(y2 - y1);
        final int signX = x1 < x2 ? 1 : -1;
        final int signY = y1 < y2 ? 1 : -1;
        int error = deltaX - deltaY;
        drawPixel(x2, y2, color);
        while (x1 != x2 || y1 != y2) {
            drawPixel(x1, y1, color);
            int error2 = error * 2;
            if (error2 > -deltaY) {
                error -= deltaY;
                x1 += signX;
            }
            if (error2 < deltaX) {
                error += deltaX;
                y1 += signY;
            }
        }
    }

    public void drawTriangle(float[] vertices, int[] indices) {
        int[] x = new int[indices.length];
        int[] y = new int[indices.length];

        for (int i = 0; i < indices.length; i++) {
            int index = indices[i];

            Vector4f vertex = new Vector4f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2], 1.0f);
            modelMatrix.transform(vertex);
            viewMatrix.transform(vertex);
            projectionMatrix.transform(vertex);

            vertex.x /= vertex.w;
            vertex.y /= vertex.w;

            x[i] = (int) ((vertex.x + 1.0) * width / 2.0);
            y[i] = (int) ((1.0 - vertex.y) * height / 2.0);
        }
        Color color = Color.red;
        for (int i = 0; i < indices.length; i += 3) {
            drawLine(color, x[0 + i], y[0 + i], x[1 + i], y[1 + i]);
            drawLine(color, x[1 + i], y[1 + i], x[2 + i], y[2 + i]);
            drawLine(color, x[2 + i], y[2 + i], x[0 + i], y[0 + i]);
        }
    }

    public void drawFilledTriangle(Texture texture, float[] vertices, int[] indices, float[] normals, float[] textureCoords) {
        for (int i = 0; i < indices.length; i += 3) {
            // Apply vertex transformation
            VertexInfo vertex0 = transformVertex(vertices, normals, textureCoords, indices[i + 0]);
            VertexInfo vertex1 = transformVertex(vertices, normals, textureCoords, indices[i + 1]);
            VertexInfo vertex2 = transformVertex(vertices, normals, textureCoords, indices[i + 2]);

            int x0 = (int) vertex0.position.x;
            int y0 = (int) vertex0.position.y;

            int x1 = (int) vertex1.position.x;
            int y1 = (int) vertex1.position.y;

            int x2 = (int) vertex2.position.x;
            int y2 = (int) vertex2.position.y;

            float z0 = vertex0.position.z;
            float z1 = vertex1.position.z;
            float z2 = vertex2.position.z;

            // Iterate over the bounding box of the triangle
            for (int pixelY = Math.min(y0, Math.min(y1, y2)); pixelY <= Math.max(y0, Math.max(y1, y2)); pixelY++) {
                for (int pixelX = Math.min(x0, Math.min(x1, x2)); pixelX <=  Math.max(x0, Math.max(x1, x2)); pixelX++) {
                    if (((x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)) < 0) { // front face checking
                        float[] barycentric = calculateBarycentricCoordinates(vertex0.position, vertex1.position, vertex2.position, pixelX, pixelY);
                        if (barycentric[0] >= 0 && barycentric[1] >= 0 && barycentric[2] >= 0) {

                            float interpolatedZ = interpolateBarycentric(z0, z1, z2, barycentric);
                            int pixelIndex = pixelX + pixelY * width;
                            if (pixelIndex < 0 || pixelIndex > depthBuffer.length - 1) {
                                continue;
                            }

                            if (interpolatedZ > depthBuffer[pixelIndex]) {
//                                float normalX = interpolateBarycentric(vertex0.normal.x, vertex1.normal.x, vertex2.normal.x, barycentric);
//                                float normalY = interpolateBarycentric(vertex0.normal.y, vertex1.normal.y, vertex2.normal.y, barycentric);
//                                float normalZ = interpolateBarycentric(vertex0.normal.z, vertex1.normal.z, vertex2.normal.z, barycentric);

                                float textureU = interpolateBarycentric(vertex0.textureU, vertex1.textureU, vertex2.textureU, barycentric);
                                float textureV = interpolateBarycentric(vertex0.textureV, vertex1.textureV, vertex2.textureV, barycentric);
                                int pixel = texture.getPixel(textureU * texture.width, textureV * texture.height);
                                if (pixel >> 24 != 0) {
                                    depthBuffer[pixelIndex] = (int) interpolatedZ;
                                    pixelData[pixelIndex] = pixel;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static class VertexInfo {

        protected Vector4f position;
        protected Vector4f normal;
        protected float textureU, textureV;

        public VertexInfo(Vector4f position, Vector4f normal, float textureU, float textureV) {
            this.position = position;
            this.normal = normal;
            this.textureU = textureU;
            this.textureV = textureV;
        }
    }

    private VertexInfo transformVertex(float[] vertices, float[] normals, float[] textureCoords, int index) {
        Vector4f vertex = new Vector4f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2], 1.0f);
        modelMatrix.transform(vertex);
        viewMatrix.transform(vertex);
        projectionMatrix.transform(vertex);

        Vector4f normal = null;
        if (normalMatrix != null) {
            // Transform normal
            normal = new Vector4f(normals[index * 3], normals[index * 3 + 1], normals[index * 3 + 2], 0.0f);  // Ensure it's a 4D vector
            normal = normal.mul(normalMatrix);  // Apply the normal matrix
            normal = normal.normalize();  // Normalize the normal vector
        }

        vertex.x /= vertex.w;
        vertex.y /= vertex.w;
        vertex.z /= vertex.w;

        int x = (int) ((vertex.x + 1.0) * width / 2.0);
        int y = (int) ((1.0 - vertex.y) * height / 2.0);

        return new VertexInfo(new Vector4f(x, y, vertex.z, vertex.w), normal, textureCoords[index * 2], textureCoords[index * 2 + 1]);
    }

    private float[] calculateBarycentricCoordinates(Vector4f vec0, Vector4f vec1, Vector4f vec2, int x, int y) {
        float detT = (vec1.y - vec2.y) * (vec0.x - vec2.x) + (vec2.x - vec1.x) * (vec0.y - vec2.y);
        float alpha = ((vec1.y - vec2.y) * (x - vec2.x) + (vec2.x - vec1.x) * (y - vec2.y)) / detT;
        float beta = ((vec2.y - vec0.y) * (x - vec2.x) + (vec0.x - vec2.x) * (y - vec2.y)) / detT;
        float gamma = 1 - alpha - beta;

        return new float[]{alpha, beta, gamma};
    }

    private float interpolateBarycentric(float v0, float v1, float v2, float[] barycentric) {
        return v0 * barycentric[0] + v1 * barycentric[1] + v2 * barycentric[2];
    }
}
