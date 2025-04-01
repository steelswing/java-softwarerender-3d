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

    public void drawFilledTriangleInline(Texture texture, float[] vertices, int[] indices, float[] normals, float[] textureCoords, int start, int count) {
        for (int i = start; i < count; i += 3) {
            if (i > indices.length - 1) {
                break;
            }
            // Трансформация первой вершины
            int idx0 = indices[i] * 3;
            float x0 = vertices[idx0];
            float y0 = vertices[idx0 + 1];
            float z0 = vertices[idx0 + 2];
            float w0 = 1.0f;

            // modelMatrix multiplication
            float mx0 = modelMatrix.m00() * x0 + modelMatrix.m10() * y0 + modelMatrix.m20() * z0 + modelMatrix.m30() * w0;
            float my0 = modelMatrix.m01() * x0 + modelMatrix.m11() * y0 + modelMatrix.m21() * z0 + modelMatrix.m31() * w0;
            float mz0 = modelMatrix.m02() * x0 + modelMatrix.m12() * y0 + modelMatrix.m22() * z0 + modelMatrix.m32() * w0;
            float mw0 = modelMatrix.m03() * x0 + modelMatrix.m13() * y0 + modelMatrix.m23() * z0 + modelMatrix.m33() * w0;

            // viewMatrix multiplication
            float vx0 = viewMatrix.m00() * mx0 + viewMatrix.m10() * my0 + viewMatrix.m20() * mz0 + viewMatrix.m30() * mw0;
            float vy0 = viewMatrix.m01() * mx0 + viewMatrix.m11() * my0 + viewMatrix.m21() * mz0 + viewMatrix.m31() * mw0;
            float vz0 = viewMatrix.m02() * mx0 + viewMatrix.m12() * my0 + viewMatrix.m22() * mz0 + viewMatrix.m32() * mw0;
            float vw0 = viewMatrix.m03() * mx0 + viewMatrix.m13() * my0 + viewMatrix.m23() * mz0 + viewMatrix.m33() * mw0;

            // projectionMatrix multiplication
            float px0 = projectionMatrix.m00() * vx0 + projectionMatrix.m10() * vy0 + projectionMatrix.m20() * vz0 + projectionMatrix.m30() * vw0;
            float py0 = projectionMatrix.m01() * vx0 + projectionMatrix.m11() * vy0 + projectionMatrix.m21() * vz0 + projectionMatrix.m31() * vw0;
            float pz0 = projectionMatrix.m02() * vx0 + projectionMatrix.m12() * vy0 + projectionMatrix.m22() * vz0 + projectionMatrix.m32() * vw0;
            float pw0 = projectionMatrix.m03() * vx0 + projectionMatrix.m13() * vy0 + projectionMatrix.m23() * vz0 + projectionMatrix.m33() * vw0;

            px0 /= pw0;
            py0 /= pw0;
            pz0 /= pw0;
            int screenX0 = (int) ((px0 + 1.0f) * width / 2.0f);
            int screenY0 = (int) ((1.0f - py0) * height / 2.0f);
            float texU0 = textureCoords[indices[i] * 2];
            float texV0 = textureCoords[indices[i] * 2 + 1];

            // Трансформация второй вершины
            int idx1 = indices[i + 1] * 3;
            float x1 = vertices[idx1];
            float y1 = vertices[idx1 + 1];
            float z1 = vertices[idx1 + 2];
            float w1 = 1.0f;

            float mx1 = modelMatrix.m00() * x1 + modelMatrix.m10() * y1 + modelMatrix.m20() * z1 + modelMatrix.m30() * w1;
            float my1 = modelMatrix.m01() * x1 + modelMatrix.m11() * y1 + modelMatrix.m21() * z1 + modelMatrix.m31() * w1;
            float mz1 = modelMatrix.m02() * x1 + modelMatrix.m12() * y1 + modelMatrix.m22() * z1 + modelMatrix.m32() * w1;
            float mw1 = modelMatrix.m03() * x1 + modelMatrix.m13() * y1 + modelMatrix.m23() * z1 + modelMatrix.m33() * w1;

            float vx1 = viewMatrix.m00() * mx1 + viewMatrix.m10() * my1 + viewMatrix.m20() * mz1 + viewMatrix.m30() * mw1;
            float vy1 = viewMatrix.m01() * mx1 + viewMatrix.m11() * my1 + viewMatrix.m21() * mz1 + viewMatrix.m31() * mw1;
            float vz1 = viewMatrix.m02() * mx1 + viewMatrix.m12() * my1 + viewMatrix.m22() * mz1 + viewMatrix.m32() * mw1;
            float vw1 = viewMatrix.m03() * mx1 + viewMatrix.m13() * my1 + viewMatrix.m23() * mz1 + viewMatrix.m33() * mw1;

            float px1 = projectionMatrix.m00() * vx1 + projectionMatrix.m10() * vy1 + projectionMatrix.m20() * vz1 + projectionMatrix.m30() * vw1;
            float py1 = projectionMatrix.m01() * vx1 + projectionMatrix.m11() * vy1 + projectionMatrix.m21() * vz1 + projectionMatrix.m31() * vw1;
            float pz1 = projectionMatrix.m02() * vx1 + projectionMatrix.m12() * vy1 + projectionMatrix.m22() * vz1 + projectionMatrix.m32() * vw1;
            float pw1 = projectionMatrix.m03() * vx1 + projectionMatrix.m13() * vy1 + projectionMatrix.m23() * vz1 + projectionMatrix.m33() * vw1;

            px1 /= pw1;
            py1 /= pw1;
            pz1 /= pw1;
            int screenX1 = (int) ((px1 + 1.0f) * width / 2.0f);
            int screenY1 = (int) ((1.0f - py1) * height / 2.0f);
            float texU1 = textureCoords[indices[i + 1] * 2];
            float texV1 = textureCoords[indices[i + 1] * 2 + 1];

            // Трансформация третьей вершины
            int idx2 = indices[i + 2] * 3;
            float x2 = vertices[idx2];
            float y2 = vertices[idx2 + 1];
            float z2 = vertices[idx2 + 2];
            float w2 = 1.0f;

            float mx2 = modelMatrix.m00() * x2 + modelMatrix.m10() * y2 + modelMatrix.m20() * z2 + modelMatrix.m30() * w2;
            float my2 = modelMatrix.m01() * x2 + modelMatrix.m11() * y2 + modelMatrix.m21() * z2 + modelMatrix.m31() * w2;
            float mz2 = modelMatrix.m02() * x2 + modelMatrix.m12() * y2 + modelMatrix.m22() * z2 + modelMatrix.m32() * w2;
            float mw2 = modelMatrix.m03() * x2 + modelMatrix.m13() * y2 + modelMatrix.m23() * z2 + modelMatrix.m33() * w2;

            float vx2 = viewMatrix.m00() * mx2 + viewMatrix.m10() * my2 + viewMatrix.m20() * mz2 + viewMatrix.m30() * mw2;
            float vy2 = viewMatrix.m01() * mx2 + viewMatrix.m11() * my2 + viewMatrix.m21() * mz2 + viewMatrix.m31() * mw2;
            float vz2 = viewMatrix.m02() * mx2 + viewMatrix.m12() * my2 + viewMatrix.m22() * mz2 + viewMatrix.m32() * mw2;
            float vw2 = viewMatrix.m03() * mx2 + viewMatrix.m13() * my2 + viewMatrix.m23() * mz2 + viewMatrix.m33() * mw2;

            float px2 = projectionMatrix.m00() * vx2 + projectionMatrix.m10() * vy2 + projectionMatrix.m20() * vz2 + projectionMatrix.m30() * vw2;
            float py2 = projectionMatrix.m01() * vx2 + projectionMatrix.m11() * vy2 + projectionMatrix.m21() * vz2 + projectionMatrix.m31() * vw2;
            float pz2 = projectionMatrix.m02() * vx2 + projectionMatrix.m12() * vy2 + projectionMatrix.m22() * vz2 + projectionMatrix.m32() * vw2;
            float pw2 = projectionMatrix.m03() * vx2 + projectionMatrix.m13() * vy2 + projectionMatrix.m23() * vz2 + projectionMatrix.m33() * vw2;

            px2 /= pw2;
            py2 /= pw2;
            pz2 /= pw2;
            int screenX2 = (int) ((px2 + 1.0f) * width / 2.0f);
            int screenY2 = (int) ((1.0f - py2) * height / 2.0f);
            float texU2 = textureCoords[indices[i + 2] * 2];
            float texV2 = textureCoords[indices[i + 2] * 2 + 1];

            // Bounding box и рендеринг
            int minX = Math.min(screenX0, Math.min(screenX1, screenX2));
            int maxX = Math.max(screenX0, Math.max(screenX1, screenX2));
            int minY = Math.min(screenY0, Math.min(screenY1, screenY2));
            int maxY = Math.max(screenY0, Math.max(screenY1, screenY2));

            if (((screenX1 - screenX0) * (screenY2 - screenY0) - (screenX2 - screenX0) * (screenY1 - screenY0)) < 0) {
                for (int pixelY = minY; pixelY <= maxY; pixelY++) {
                    for (int pixelX = minX; pixelX <= maxX; pixelX++) {
                        float detT = (screenY1 - screenY2) * (screenX0 - screenX2) + (screenX2 - screenX1) * (screenY0 - screenY2);
                        float alpha = ((screenY1 - screenY2) * (pixelX - screenX2) + (screenX2 - screenX1) * (pixelY - screenY2)) / detT;
                        float beta = ((screenY2 - screenY0) * (pixelX - screenX2) + (screenX0 - screenX2) * (pixelY - screenY2)) / detT;
                        float gamma = 1 - alpha - beta;

                        if (alpha >= 0 && beta >= 0 && gamma >= 0) {
                            float interpolatedZ = pz0 * alpha + pz1 * beta + pz2 * gamma;
                            int pixelIndex = pixelX + pixelY * width;

                            if (pixelIndex >= 0 && pixelIndex < depthBuffer.length && interpolatedZ > depthBuffer[pixelIndex]) {
                                float textureU = texU0 * alpha + texU1 * beta + texU2 * gamma;
                                float textureV = texV0 * alpha + texV1 * beta + texV2 * gamma;
                                int pixel = texture.getPixel(textureU * texture.width, textureV * texture.height);
                                if (pixel >> 24 != 0) {
                                    depthBuffer[pixelIndex] = interpolatedZ;
                                    pixelData[pixelIndex] = pixel;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
