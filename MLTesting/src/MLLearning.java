/**
 * Created by phrayezzen on 4/12/15.
 */
import com.sun.javafx.geom.DirtyRegionPool;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * XOR: This example is essentially the "Hello World" of neural network
 * programming.  This example shows how to construct an Encog neural
 * network to predict the output from the XOR operator.  This example
 * uses backpropagation to train the neural network.
 *
 * This example attempts to use a minimum of Encog features to create and
 * train the neural network.  This allows you to see exactly what is going
 * on.  For a more advanced example, that uses Encog factories, refer to
 * the XORFactory example.
 *
 */
public class MLLearning {

    private double[][] SAMPLE = null;
    private double[][] IDEAL = { {0}, {0}, {0}, {0}, {0}, {0}, {1}, {1}, {1}, {1}, {1}, {1} };
    private final String[] BANDS = { "alpha", "beta", "gamma", "theta" };
    private static String DIRPATH = "/Users/phrayezzen/Downloads/push samples/";
    private BasicNetwork network;

    public MLLearning(String dirpath) {
        DIRPATH = dirpath;
        initializeNetwork();
        loadFiles();
    }

    private void initializeNetwork() {
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(null, true, 30));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 30));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
        network.getStructure().finalizeStructure();
        network.reset();
    }

    private void loadFiles() {
        try {
            for (int i = 0; i < IDEAL.length; i++)
                for (int j = 0; j < BANDS.length; j++) {
                    String filename = DIRPATH + "s" + i + "/" + BANDS[j] + ".txt";
                    List<String> lines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());
                    if (SAMPLE == null)
                        SAMPLE = new double[IDEAL.length][BANDS.length * lines.size()];
                    for (int k = 0; k < lines.size(); k++)
                        if (j * lines.size() + k >= SAMPLE[0].length)
                            break;
                        else
                            SAMPLE[i][j * lines.size() + k] = Double.parseDouble(lines.get(k));
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void train() {
        // create training data
        MLDataSet trainingSet = new BasicMLDataSet(SAMPLE, IDEAL);

        // train the neural network
        final ResilientPropagation train = new ResilientPropagation(network, trainingSet);

        int epoch = 1;

        do {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());
            epoch++;
        } while(train.getError() > 0.01);
        train.finishTraining();
    }

    public void saveNetwork() {
        EncogDirectoryPersistence.saveObject(new File(DIRPATH + "network"), network);
    }

    public double test(double[] input) {
        // test the neural network
        System.out.println("Neural Network Results:");
        final MLData output = network.compute(new BasicMLData(input));
        return output.getData(0);
    }

    public void shutdown() {
        Encog.getInstance().shutdown();
    }

    public void printSample() {
        for (double[] aSAMPLE : SAMPLE) {
            for (int j = 0; j < aSAMPLE.length; j++)
                System.out.print(aSAMPLE[j] + " ");
            System.out.println("");
        }
    }

    public static void main(String[] args) {
        MLLearning l = new MLLearning(DIRPATH);
        l.train();
        l.saveNetwork();
    }
}
