import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import net.steelswing.engine.api.util.MathHelper;
import net.steelswing.engine.api.vecmath.MathUtil;
import org.joml.Matrix4f;

/**
 *
 * @author LWJGL2
 */
public class Example {

    protected static int threadCount = 12;
    protected static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(856, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.createBufferStrategy(1);

        final Buffer buffer = new Buffer(frame.getWidth(), frame.getHeight());
        final BufferStrategy bufferStrategy = frame.getBufferStrategy();
        final Graphics g = bufferStrategy.getDrawGraphics();

        long time = System.nanoTime();
        long secondInNanos = TimeUnit.SECONDS.toNanos(1);
        int frames = 0;
        init();

        while (frame.isVisible()) {
            if (buffer.width != frame.getWidth() || buffer.height != frame.getHeight()) {
                buffer.create(frame.getWidth(), frame.getHeight());
            }
            buffer.clear(Color.black);
            {
                render(buffer);
            }

            buffer.draw(g, frame.getWidth(), frame.getHeight());
            frames++;
            if (System.nanoTime() - time > secondInNanos) {
                time = System.nanoTime();
                buffer.fps = frames;
                frame.setTitle("FPS: " + buffer.fps);
                frames = 0;
            }
        }
    }

    protected static List<StaticMeshLoader.Mesh> model;
    protected static Texture texture;

    protected static void init() {
        try {
            model = StaticMeshLoader.load(new File("teapot\\debug.fbx").getAbsoluteFile());
            texture = new Texture(ImageIO.read(new File("teapot\\debug.png").getAbsoluteFile()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void render(Buffer buffer) {
        buffer.viewMatrix = MathUtil.convertLwjglxToJoml(MathUtil.createViewMatrix(0, 0, 0, 25, System.nanoTime() / 10000000 % 360, 0, 2));
        buffer.projectionMatrix = MathUtil.convertLwjglxToJoml(MathUtil.createProjectionMatrix(1000, -100, 70, buffer.width, buffer.height));
// buffer.normalMatrix = new Matrix4f(buffer.modelMatrix).invert().transpose();

        buffer.modelMatrix = new Matrix4f().translate(0, -0.5f, 0);//.rotateX(MathHelper.toRadians(-90));

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (StaticMeshLoader.Mesh mesh : model) {
            final int count = mesh.indices.length / threadCount;

            for (int i = 0; i < mesh.indices.length; i += count) {
                final int start = i;

                CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                    buffer.drawFilledTriangleInline(texture, mesh.vertices, mesh.indices, mesh.normals, mesh.textureCoords, start, start + count);
                }, executor);

                futures.add(runAsync);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        /* single thread
        for (StaticMeshLoader.Mesh mesh : model) {
            System.out.println( mesh.vertices.length);
            buffer.drawFilledTriangleInline(texture, mesh.vertices, mesh.indices, mesh.normals, mesh.textureCoords);
        }*/
    }


}
