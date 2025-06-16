package beast.base.inference.distribution;

import beast.base.inference.parameter.RealParameter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DirichletSimulator {

    static String homePath = System.getProperty("user.home");
    static Path outFilePath = Paths.get(homePath, "WorkSpace", "tmp", "simDirichletA1111.log");

    public static void main(String[] args) {
        Dirichlet dirichlet = new Dirichlet();
        RealParameter alphaParam = new RealParameter(new Double[]{1.0,1.0,1.0,1.0});

        dirichlet.initByName("alpha", alphaParam);

        final int size = 100000;
        Double[][] val2d = dirichlet.sample(size);
        System.out.println("Simulate " + size + " samples");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFilePath.toFile()))) {
            // header
            writer.write("Sample\t");
            if (alphaParam.getDimension() == 4) {
                writer.write("pi.A\tpi.C\tpi.G\tpi.T");
            } else {
                for (int i = 0; i < val2d[0].length; i++) {
                    writer.write("Var" + (i + 1));
                    if (i < val2d[0].length - 1) {
                        writer.write('\t'); // tab delimiter
                    }
                }
            }
            writer.newLine(); // new line after each row

            for (int n = 0; n < val2d.length; n++) {
                writer.write(Integer.toString(n) + '\t');
                for (int i = 0; i < val2d[n].length; i++) {
                    writer.write(Double.toString(val2d[n][i]));
                    if (i < val2d[n].length - 1) {
                        writer.write('\t'); // tab delimiter
                    }
                }
                writer.newLine(); // new line after each row

                if (n % 1000 == 0) {
                    System.out.println("Wrote " + n + " lines...");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Finish writing file to " + outFilePath.toFile());
    }

}
