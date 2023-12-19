import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Classifier {
    private Interpreter interpreter;
    private List<String> labelList;
    private final int inputSize;
    private static final int PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;
    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.4f;

    public static class Recognition {
        public String id = "";
        public String title = "";
        public float confidence = 0F;

        @Override
        public String toString() {
            return "Title = " + title + ", Confidence = " + confidence;
        }
    }

    public Classifier(AssetManager assetManager, String modelPath, String labelPath, int inputSize) {
        this.interpreter = new Interpreter(loadModelFile(assetManager, modelPath));
        this.labelList = loadLabelList(assetManager, labelPath);
        this.inputSize = inputSize;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(modelPath);
            FileChannel fileChannel = fileInputStream.getChannel();
            long startOffset = fileChannel.position();
            long declaredLength = fileChannel.size();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) {
        List<String> labelList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
            String line;
            while ((line = reader.readLine()) != null) {
                labelList.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labelList;
    }

    public List<Recognition> recognizeImage(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);
        ByteBuffer byteBuffer = bitmapToByteBuffer(scaledBitmap);
        float[][] result = new float[1][labelList.size()];
        interpreter.run(byteBuffer, result);
        return getSortedResult(result);
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat(((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    private List<Recognition> getSortedResult(float[][] labelProbArray) {
        PriorityQueue<Recognition> pq = new PriorityQueue<>(
                MAX_RESULTS,
                (Recognition r1, Recognition r2) -> Float.compare(r2.confidence, r1.confidence));

        for (int i = 0; i < labelList.size(); i++) {
            float confidence = labelProbArray[0][i];
            if (confidence >= THRESHOLD) {
                pq.add(new Recognition(String.valueOf(i),
                        i < labelList.size() ? labelList.get(i) : "Unknown", confidence));
            }
        }

        List<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; i++) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }
}
